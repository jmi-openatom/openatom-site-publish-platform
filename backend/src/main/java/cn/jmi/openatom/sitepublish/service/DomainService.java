package cn.jmi.openatom.sitepublish.service;

import cn.dev33.satoken.stp.StpUtil;
import cn.jmi.openatom.sitepublish.common.BusinessException;
import cn.jmi.openatom.sitepublish.config.DomainProperties;
import cn.jmi.openatom.sitepublish.dto.SiteDtos;
import cn.jmi.openatom.sitepublish.entity.Site;
import cn.jmi.openatom.sitepublish.entity.SiteDomain;
import cn.jmi.openatom.sitepublish.mapper.SiteDomainMapper;
import cn.jmi.openatom.sitepublish.mapper.SiteMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class DomainService {

    private final SiteService siteService;
    private final SiteMapper siteMapper;
    private final SiteDomainMapper domainMapper;
    private final DnsResolver dnsResolver;
    private final DomainProperties domainProperties;

    public List<SiteDtos.DomainView> list() {
        Long userId = StpUtil.getLoginIdAsLong();
        return domainMapper.selectList(Wrappers.<SiteDomain>lambdaQuery()
                        .eq(SiteDomain::getUserId, userId)
                        .orderByDesc(SiteDomain::getUpdatedAt))
                .stream()
                .map(this::toView)
                .toList();
    }

    @Transactional
    public SiteDtos.DomainView bind(Long siteId, SiteDtos.DomainRequest request) {
        Site site = siteService.requireOwned(siteId);
        String domain = request.domain().trim().toLowerCase();
        Long duplicate = domainMapper.selectCount(Wrappers.<SiteDomain>lambdaQuery()
                .eq(SiteDomain::getDomain, domain)
                .ne(SiteDomain::getSiteId, siteId));
        if (duplicate > 0) {
            throw new BusinessException("该域名已经绑定到其他项目");
        }
        SiteDomain binding = domainMapper.selectOne(Wrappers.<SiteDomain>lambdaQuery()
                .eq(SiteDomain::getSiteId, siteId)
                .eq(SiteDomain::getType, "CUSTOM"));
        if (binding == null) {
            binding = new SiteDomain();
            binding.setSiteId(siteId);
            binding.setUserId(site.getUserId());
            binding.setType("CUSTOM");
            binding.setCreatedAt(LocalDateTime.now());
        }
        binding.setDomain(domain);
        binding.setStatus("PENDING");
        binding.setVerificationToken(randomToken());
        binding.setUpdatedAt(LocalDateTime.now());
        if (binding.getId() == null) {
            domainMapper.insert(binding);
        } else {
            domainMapper.updateById(binding);
        }
        site.setCustomDomain(null);
        site.setUpdatedAt(LocalDateTime.now());
        siteMapper.updateById(site);
        return toView(binding);
    }

    @Transactional
    public SiteDtos.DomainView verify(Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        SiteDomain binding = domainMapper.selectOne(Wrappers.<SiteDomain>lambdaQuery()
                .eq(SiteDomain::getId, id)
                .eq(SiteDomain::getUserId, userId));
        if (binding == null) {
            throw new BusinessException("域名绑定不存在");
        }
        String expectedTarget = cnameTarget(binding.getSiteId());
        List<String> cnameRecords = dnsResolver.lookupCname(binding.getDomain());
        boolean matched = cnameRecords.stream()
                .map(DomainService::normalizeDnsName)
                .anyMatch(normalizeDnsName(expectedTarget)::equals);
        if (!matched) {
            String detected = cnameRecords.isEmpty() ? "未检测到 CNAME 记录" : String.join(", ", cnameRecords);
            throw new BusinessException("CNAME 尚未生效，请将 " + binding.getDomain()
                    + " 指向 " + expectedTarget + "；当前检测结果：" + detected);
        }

        binding.setStatus("ACTIVE");
        binding.setUpdatedAt(LocalDateTime.now());
        domainMapper.updateById(binding);

        Site site = siteMapper.selectById(binding.getSiteId());
        if (site != null) {
            site.setCustomDomain(binding.getDomain());
            site.setUpdatedAt(LocalDateTime.now());
            siteMapper.updateById(site);
        }
        return toView(binding);
    }

    private SiteDtos.DomainView toView(SiteDomain domain) {
        Site site = siteMapper.selectById(domain.getSiteId());
        return new SiteDtos.DomainView(
                domain.getId(),
                domain.getSiteId(),
                site == null ? null : site.getName(),
                domain.getDomain(),
                domain.getType(),
                domain.getStatus(),
                domain.getVerificationToken(),
                cnameTarget(domain.getSiteId()),
                domain.getCreatedAt(),
                domain.getUpdatedAt()
        );
    }

    private String cnameTarget(Long siteId) {
        Site site = siteMapper.selectById(siteId);
        return domainProperties.cnameTarget(site == null ? null : site.getSlug());
    }

    static String normalizeDnsName(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        return normalized.endsWith(".") ? normalized.substring(0, normalized.length() - 1) : normalized;
    }

    private String randomToken() {
        byte[] bytes = new byte[12];
        new SecureRandom().nextBytes(bytes);
        return "site-verify-" + HexFormat.of().formatHex(bytes);
    }
}

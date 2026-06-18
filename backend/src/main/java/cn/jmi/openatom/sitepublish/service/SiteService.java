package cn.jmi.openatom.sitepublish.service;

import cn.dev33.satoken.stp.StpUtil;
import cn.jmi.openatom.sitepublish.common.BusinessException;
import cn.jmi.openatom.sitepublish.config.DomainProperties;
import cn.jmi.openatom.sitepublish.dto.SiteDtos;
import cn.jmi.openatom.sitepublish.entity.Deployment;
import cn.jmi.openatom.sitepublish.entity.Site;
import cn.jmi.openatom.sitepublish.mapper.DeploymentMapper;
import cn.jmi.openatom.sitepublish.mapper.SiteDomainMapper;
import cn.jmi.openatom.sitepublish.mapper.SiteMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SiteService {

    private final SiteMapper siteMapper;
    private final DeploymentMapper deploymentMapper;
    private final SiteDomainMapper domainMapper;
    private final DomainProperties domainProperties;

    public List<SiteDtos.SiteView> list(String keyword, String status, String framework) {
        Long userId = StpUtil.getLoginIdAsLong();
        LambdaQueryWrapper<Site> query = Wrappers.<Site>lambdaQuery()
                .eq(Site::getUserId, userId)
                .and(StringUtils.hasText(keyword), wrapper -> wrapper
                        .like(Site::getName, keyword)
                        .or()
                        .like(Site::getSlug, keyword))
                .eq(StringUtils.hasText(status), Site::getStatus, status)
                .eq(StringUtils.hasText(framework), Site::getFramework, framework)
                .orderByDesc(Site::getUpdatedAt);
        return siteMapper.selectList(query).stream().map(this::toView).toList();
    }

    public SiteDtos.SiteView get(Long id) {
        return toView(requireOwned(id));
    }

    @Transactional
    public SiteDtos.SiteView create(SiteDtos.CreateSiteRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        Long duplicate = siteMapper.selectCount(Wrappers.<Site>lambdaQuery().eq(Site::getSlug, request.slug()));
        if (duplicate > 0) {
            throw new BusinessException("项目标识已被使用");
        }

        Site site = new Site();
        site.setUserId(userId);
        site.setName(request.name().trim());
        site.setSlug(request.slug().trim());
        site.setFramework(normalizeFramework(request.framework()));
        site.setDescription(request.description());
        site.setStatus("PENDING");
        site.setDefaultDomain(request.slug() + "." + domainProperties.publicBaseDomain());
        site.setPreviewImage(previewFor(request.framework()));
        site.setBranchName("main");
        site.setCreatedAt(LocalDateTime.now());
        site.setUpdatedAt(LocalDateTime.now());
        siteMapper.insert(site);
        return toView(site);
    }

    @Transactional
    public void delete(Long id) {
        Site site = requireOwned(id);
        deploymentMapper.delete(Wrappers.<Deployment>lambdaQuery().eq(Deployment::getSiteId, site.getId()));
        domainMapper.delete(Wrappers.lambdaQuery(cn.jmi.openatom.sitepublish.entity.SiteDomain.class)
                .eq(cn.jmi.openatom.sitepublish.entity.SiteDomain::getSiteId, site.getId()));
        siteMapper.deleteById(site.getId());
    }

    public Site requireOwned(Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        Site site = siteMapper.selectOne(Wrappers.<Site>lambdaQuery()
                .eq(Site::getId, id)
                .eq(Site::getUserId, userId));
        if (site == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "项目不存在");
        }
        return site;
    }

    public SiteDtos.SiteView toView(Site site) {
        Deployment latest = site.getLatestDeploymentId() == null
                ? null
                : deploymentMapper.selectById(site.getLatestDeploymentId());
        return new SiteDtos.SiteView(
                site.getId(),
                site.getName(),
                site.getSlug(),
                site.getFramework(),
                site.getDescription(),
                site.getStatus(),
                site.getDefaultDomain(),
                domainProperties.cnameTarget(site.getSlug()),
                site.getCustomDomain(),
                "/published/" + site.getSlug() + "/",
                site.getPreviewImage(),
                site.getBranchName(),
                site.getSourceFilename(),
                site.getCreatedAt(),
                site.getUpdatedAt(),
                latest == null ? null : toDeploymentView(latest, site)
        );
    }

    public SiteDtos.DeploymentView toDeploymentView(Deployment deployment, Site site) {
        return new SiteDtos.DeploymentView(
                deployment.getId(),
                deployment.getSiteId(),
                site == null ? null : site.getName(),
                site == null ? null : site.getSlug(),
                deployment.getStatus(),
                deployment.getEnvironment(),
                deployment.getCommitHash(),
                deployment.getSourceFilename(),
                deployment.getBuildLog(),
                deployment.getDurationSeconds(),
                deployment.getStartedAt(),
                deployment.getFinishedAt(),
                deployment.getCreatedAt()
        );
    }

    private String normalizeFramework(String framework) {
        return switch (framework.toUpperCase()) {
            case "VUE", "VUE3", "VUE 3" -> "Vue 3";
            case "REACT" -> "React";
            default -> "HTML";
        };
    }

    private String previewFor(String framework) {
        return switch (normalizeFramework(framework)) {
            case "Vue 3" -> "/previews/lab.png";
            case "React" -> "/previews/recruit.png";
            default -> "/previews/mirror.png";
        };
    }
}

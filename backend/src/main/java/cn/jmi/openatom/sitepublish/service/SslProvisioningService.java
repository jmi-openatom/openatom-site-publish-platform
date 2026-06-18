package cn.jmi.openatom.sitepublish.service;

import cn.jmi.openatom.sitepublish.config.SslProperties;
import cn.jmi.openatom.sitepublish.entity.Site;
import cn.jmi.openatom.sitepublish.entity.SiteDomain;
import cn.jmi.openatom.sitepublish.mapper.SiteDomainMapper;
import cn.jmi.openatom.sitepublish.mapper.SiteMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.net.ssl.SSLHandshakeException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class SslProvisioningService {

    private final SiteDomainMapper domainMapper;
    private final SiteMapper siteMapper;
    private final SslProperties properties;
    private final TlsCertificateClient certificateClient;
    private final Map<Long, SslState> states = new ConcurrentHashMap<>();
    private final Set<Long> inFlight = ConcurrentHashMap.newKeySet();

    public void reset(Long domainId) {
        states.remove(domainId);
    }

    public void markProvisioning(Long domainId) {
        states.put(domainId, new SslState(
                "SSL_PROVISIONING",
                "正在向证书颁发机构申请证书",
                null
        ));
    }

    public SslState status(SiteDomain binding, Site site) {
        if (!"ACTIVE".equals(binding.getStatus())) {
            return new SslState("SSL_WAITING_DNS", "请先完成 CNAME 验证", null);
        }
        if (!properties.enabled()) {
            return new SslState("SSL_DISABLED", "服务器尚未启用自动 SSL 入口", null);
        }
        if (site == null || site.getLatestDeploymentId() == null || !"ONLINE".equals(site.getStatus())) {
            return new SslState("SSL_WAITING_SITE", "站点上线后将自动申请证书", null);
        }
        return states.getOrDefault(binding.getId(), new SslState(
                "SSL_PROVISIONING",
                "证书任务等待执行",
                null
        ));
    }

    public void provision(Long domainId) {
        if (!inFlight.add(domainId)) {
            return;
        }
        try {
            SiteDomain binding = domainMapper.selectById(domainId);
            if (binding == null || !"ACTIVE".equals(binding.getStatus())) {
                states.put(domainId, new SslState("SSL_WAITING_DNS", "请先完成 CNAME 验证", null));
                return;
            }
            if (!properties.enabled()) {
                states.put(domainId, new SslState("SSL_DISABLED", "服务器尚未启用自动 SSL 入口", null));
                return;
            }

            Site site = siteMapper.selectById(binding.getSiteId());
            if (site == null || site.getLatestDeploymentId() == null || !"ONLINE".equals(site.getStatus())) {
                states.put(domainId, new SslState(
                        "SSL_WAITING_SITE",
                        "站点上线后将自动申请证书",
                        null
                ));
                return;
            }

            markProvisioning(domainId);
            TlsCertificateClient.CertificateResult certificate =
                    certificateClient.request(binding.getDomain(), properties);
            states.put(domainId, new SslState(
                    "SSL_ACTIVE",
                    "HTTPS 已启用",
                    certificate.expiresAt()
            ));
            log.info("SSL certificate is active for {} until {}",
                    binding.getDomain(), certificate.expiresAt());
        } catch (Exception exception) {
            String message = friendlyError(exception);
            states.put(domainId, new SslState("SSL_FAILED", message, null));
            log.warn("SSL provisioning failed for domain id {}: {}", domainId, message, exception);
        } finally {
            inFlight.remove(domainId);
        }
    }

    public void provisionForSite(Long siteId) {
        domainMapper.selectList(Wrappers.<SiteDomain>lambdaQuery()
                        .eq(SiteDomain::getSiteId, siteId)
                        .eq(SiteDomain::getStatus, "ACTIVE"))
                .forEach(binding -> provision(binding.getId()));
    }

    public void reconcile() {
        if (!properties.enabled()) {
            return;
        }
        List<SiteDomain> activeDomains = domainMapper.selectList(
                Wrappers.<SiteDomain>lambdaQuery().eq(SiteDomain::getStatus, "ACTIVE")
        );
        activeDomains.stream()
                .filter(binding -> {
                    SslState state = states.get(binding.getId());
                    return state == null
                            || !"SSL_ACTIVE".equals(state.status())
                            || state.expiresAt() == null
                            || state.expiresAt().isBefore(LocalDateTime.now().plusDays(30));
                })
                .forEach(binding -> provision(binding.getId()));
    }

    private String friendlyError(Exception exception) {
        Throwable cause = exception;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        if (cause instanceof ConnectException) {
            return "无法连接自动 SSL 入口，请检查 Caddy 是否运行";
        }
        if (cause instanceof SocketTimeoutException) {
            return "证书申请超时，将自动重试";
        }
        if (exception instanceof SSLHandshakeException || cause instanceof SSLHandshakeException) {
            return "证书签发失败，请检查 DNS 是否指向本服务器及 80/443 端口";
        }
        return "证书申请失败，将自动重试";
    }

    public record SslState(String status, String message, LocalDateTime expiresAt) {
    }
}

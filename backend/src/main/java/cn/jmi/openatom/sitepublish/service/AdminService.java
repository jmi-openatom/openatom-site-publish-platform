package cn.jmi.openatom.sitepublish.service;

import cn.jmi.openatom.sitepublish.common.BusinessException;
import cn.jmi.openatom.sitepublish.config.DomainProperties;
import cn.jmi.openatom.sitepublish.dto.AdminDtos;
import cn.jmi.openatom.sitepublish.entity.Deployment;
import cn.jmi.openatom.sitepublish.entity.Site;
import cn.jmi.openatom.sitepublish.entity.SiteDomain;
import cn.jmi.openatom.sitepublish.entity.User;
import cn.jmi.openatom.sitepublish.mapper.DeploymentMapper;
import cn.jmi.openatom.sitepublish.mapper.SiteDomainMapper;
import cn.jmi.openatom.sitepublish.mapper.SiteMapper;
import cn.jmi.openatom.sitepublish.mapper.UserMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final AdminAccessService adminAccessService;
    private final UserMapper userMapper;
    private final SiteMapper siteMapper;
    private final DeploymentMapper deploymentMapper;
    private final SiteDomainMapper domainMapper;
    private final SiteService siteService;
    private final DomainProperties domainProperties;
    private final SslProvisioningService sslProvisioningService;

    public AdminDtos.DashboardView dashboard() {
        User currentAdmin = adminAccessService.requireAdmin();
        List<User> users = userMapper.selectList(Wrappers.<User>lambdaQuery().orderByDesc(User::getCreatedAt));
        List<Site> sites = siteMapper.selectList(Wrappers.<Site>lambdaQuery().orderByDesc(Site::getUpdatedAt));
        List<SiteDomain> domains = domainMapper.selectList(
                Wrappers.<SiteDomain>lambdaQuery().orderByDesc(SiteDomain::getUpdatedAt)
        );
        List<Deployment> deployments = deploymentMapper.selectList(
                Wrappers.<Deployment>lambdaQuery()
                        .orderByDesc(Deployment::getCreatedAt)
                        .last("LIMIT 100")
        );

        Map<Long, User> usersById = users.stream()
                .filter(user -> user.getId() != null)
                .collect(Collectors.toMap(User::getId, Function.identity()));
        Map<Long, Site> sitesById = sites.stream()
                .filter(site -> site.getId() != null)
                .collect(Collectors.toMap(Site::getId, Function.identity()));
        Map<Long, Long> siteCounts = sites.stream()
                .collect(Collectors.groupingBy(Site::getUserId, Collectors.counting()));

        AdminDtos.SummaryView summary = new AdminDtos.SummaryView(
                users.size(),
                sites.size(),
                sites.stream().filter(site -> "ONLINE".equals(site.getStatus())).count(),
                deploymentMapper.selectCount(Wrappers.emptyWrapper()),
                deploymentMapper.selectCount(Wrappers.<Deployment>lambdaQuery()
                        .eq(Deployment::getStatus, "SUCCESS")),
                deploymentMapper.selectCount(Wrappers.<Deployment>lambdaQuery()
                        .eq(Deployment::getStatus, "FAILED")),
                domains.size(),
                domains.stream().filter(domain -> "ACTIVE".equals(domain.getStatus())).count()
        );

        return new AdminDtos.DashboardView(
                currentAdmin.getId(),
                summary,
                users.stream().map(user -> toUserView(user, siteCounts.getOrDefault(user.getId(), 0L))).toList(),
                sites.stream().map(site -> toSiteView(site, usersById.get(site.getUserId()))).toList(),
                domains.stream().map(domain -> toDomainView(
                        domain,
                        sitesById.get(domain.getSiteId()),
                        usersById.get(domain.getUserId())
                )).toList(),
                deployments.stream().map(deployment -> {
                    Site site = sitesById.get(deployment.getSiteId());
                    User owner = usersById.get(deployment.getUserId());
                    return toDeploymentView(deployment, site, owner);
                }).toList()
        );
    }

    @Transactional
    public AdminDtos.UserView updateAdminRole(Long userId, boolean enabled) {
        User currentAdmin = adminAccessService.requireAdmin();
        User target = userMapper.selectById(userId);
        if (target == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "用户不存在");
        }
        if (Objects.equals(currentAdmin.getId(), target.getId()) && !enabled) {
            throw new BusinessException("不能取消当前账号的管理员权限");
        }

        LinkedHashSet<String> roles = AdminAccessService.parseRoles(target.getRoles());
        roles.removeIf(AdminAccessService::isManagedAdminRole);
        if (roles.isEmpty()) {
            roles.add("member");
        }
        if (enabled) {
            roles.add("site_admin");
        }
        target.setRoles(String.join(",", roles));
        target.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(target);

        long siteCount = siteMapper.selectCount(
                Wrappers.<Site>lambdaQuery().eq(Site::getUserId, target.getId())
        );
        return toUserView(target, siteCount);
    }

    @Transactional
    public void deleteSite(Long siteId) {
        adminAccessService.requireAdmin();
        Site site = siteMapper.selectById(siteId);
        if (site == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "项目不存在");
        }
        siteService.deleteAsAdmin(site);
    }

    private AdminDtos.UserView toUserView(User user, long siteCount) {
        LinkedHashSet<String> roles = AdminAccessService.parseRoles(user.getRoles());
        return new AdminDtos.UserView(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getEmail(),
                user.getAvatar(),
                List.copyOf(roles),
                AdminAccessService.hasAdminRole(user.getRoles()),
                siteCount,
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    private AdminDtos.SiteView toSiteView(Site site, User owner) {
        return new AdminDtos.SiteView(
                site.getId(),
                site.getName(),
                site.getSlug(),
                site.getFramework(),
                site.getStatus(),
                site.getDefaultDomain(),
                site.getCustomDomain(),
                site.getUserId(),
                displayName(owner),
                owner == null ? null : owner.getEmail(),
                site.getUpdatedAt()
        );
    }

    private AdminDtos.DomainView toDomainView(SiteDomain domain, Site site, User owner) {
        SslProvisioningService.SslState sslState = sslProvisioningService.status(domain, site);
        return new AdminDtos.DomainView(
                domain.getId(),
                domain.getSiteId(),
                site == null ? null : site.getName(),
                domain.getUserId(),
                displayName(owner),
                domain.getDomain(),
                domain.getStatus(),
                domainProperties.cnameTarget(site == null ? null : site.getSlug()),
                sslState.status(),
                sslState.message(),
                domain.getUpdatedAt()
        );
    }

    private AdminDtos.DeploymentView toDeploymentView(Deployment deployment, Site site, User owner) {
        return new AdminDtos.DeploymentView(
                deployment.getId(),
                deployment.getSiteId(),
                site == null ? null : site.getName(),
                deployment.getUserId(),
                displayName(owner),
                deployment.getStatus(),
                deployment.getEnvironment(),
                deployment.getCommitHash(),
                deployment.getSourceFilename(),
                deployment.getDurationSeconds(),
                deployment.getCreatedAt()
        );
    }

    private String displayName(User user) {
        if (user == null) {
            return "未知用户";
        }
        if (user.getDisplayName() != null && !user.getDisplayName().isBlank()) {
            return user.getDisplayName();
        }
        return user.getUsername();
    }
}

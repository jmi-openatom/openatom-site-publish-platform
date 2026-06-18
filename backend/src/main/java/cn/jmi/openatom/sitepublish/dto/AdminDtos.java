package cn.jmi.openatom.sitepublish.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

public final class AdminDtos {

    private AdminDtos() {
    }

    public record DashboardView(
            Long currentUserId,
            SummaryView summary,
            List<UserView> users,
            List<SiteView> sites,
            List<DomainView> domains,
            List<DeploymentView> deployments
    ) {
    }

    public record SummaryView(
            long users,
            long sites,
            long onlineSites,
            long deployments,
            long successfulDeployments,
            long failedDeployments,
            long domains,
            long activeDomains
    ) {
    }

    public record UserView(
            Long id,
            String username,
            String displayName,
            String email,
            String avatar,
            List<String> roles,
            boolean admin,
            long siteCount,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
    }

    public record SiteView(
            Long id,
            String name,
            String slug,
            String framework,
            String status,
            String defaultDomain,
            String customDomain,
            Long ownerId,
            String ownerName,
            String ownerEmail,
            LocalDateTime updatedAt
    ) {
    }

    public record DomainView(
            Long id,
            Long siteId,
            String siteName,
            Long ownerId,
            String ownerName,
            String domain,
            String status,
            String cnameTarget,
            String sslStatus,
            String sslMessage,
            LocalDateTime updatedAt
    ) {
    }

    public record DeploymentView(
            Long id,
            Long siteId,
            String siteName,
            Long ownerId,
            String ownerName,
            String status,
            String environment,
            String commitHash,
            String sourceFilename,
            Integer durationSeconds,
            LocalDateTime createdAt
    ) {
    }

    public record UpdateAdminRoleRequest(
            @NotNull(message = "请指定管理员状态")
            Boolean enabled
    ) {
    }
}

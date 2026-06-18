package cn.jmi.openatom.sitepublish.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public final class SiteDtos {

    private SiteDtos() {
    }

    public record CreateSiteRequest(
            @NotBlank(message = "请输入项目名称")
            @Size(max = 80, message = "项目名称最多 80 个字符")
            String name,
            @NotBlank(message = "请输入项目标识")
            @Pattern(regexp = "^[a-z0-9][a-z0-9-]{1,48}[a-z0-9]$", message = "项目标识只能使用小写字母、数字和连字符")
            String slug,
            @NotBlank(message = "请选择项目框架")
            String framework,
            @Size(max = 240, message = "项目说明最多 240 个字符")
            String description
    ) {
    }

    public record SiteView(
            Long id,
            String name,
            String slug,
            String framework,
            String description,
            String status,
            String defaultDomain,
            String cnameTarget,
            String customDomain,
            String publicUrl,
            String previewImage,
            String branchName,
            String sourceFilename,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            DeploymentView latestDeployment
    ) {
    }

    public record DeploymentView(
            Long id,
            Long siteId,
            String siteName,
            String siteSlug,
            String status,
            String environment,
            String commitHash,
            String sourceFilename,
            String buildLog,
            Integer durationSeconds,
            LocalDateTime startedAt,
            LocalDateTime finishedAt,
            LocalDateTime createdAt
    ) {
    }

    public record DomainRequest(
            @NotBlank(message = "请输入自定义域名")
            @Pattern(regexp = "^(?=.{3,253}$)([a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?\\.)+[a-zA-Z]{2,}$", message = "域名格式不正确")
            String domain
    ) {
    }

    public record DomainView(
            Long id,
            Long siteId,
            String siteName,
            String domain,
            String type,
            String status,
            String verificationToken,
            String cnameTarget,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
    }
}

package cn.jmi.openatom.sitepublish.controller;

import cn.jmi.openatom.sitepublish.common.ApiResponse;
import cn.jmi.openatom.sitepublish.dto.SiteDtos;
import cn.jmi.openatom.sitepublish.service.DeploymentService;
import cn.jmi.openatom.sitepublish.service.SiteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/sites")
@RequiredArgsConstructor
public class SiteController {

    private final SiteService siteService;
    private final DeploymentService deploymentService;

    @GetMapping
    public ApiResponse<List<SiteDtos.SiteView>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String framework
    ) {
        return ApiResponse.ok(siteService.list(keyword, status, framework));
    }

    @GetMapping("/{id}")
    public ApiResponse<SiteDtos.SiteView> get(@PathVariable Long id) {
        return ApiResponse.ok(siteService.get(id));
    }

    @PostMapping
    public ApiResponse<SiteDtos.SiteView> create(@Valid @RequestBody SiteDtos.CreateSiteRequest request) {
        return ApiResponse.ok(siteService.create(request), "项目已创建");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        siteService.delete(id);
        return ApiResponse.message("项目已删除");
    }

    @PostMapping("/{id}/deploy")
    public ApiResponse<SiteDtos.DeploymentView> deploy(
            @PathVariable Long id,
            @RequestPart("file") MultipartFile file
    ) {
        return ApiResponse.ok(deploymentService.uploadAndDeploy(id, file), "项目已进入构建队列");
    }

    @PostMapping("/{id}/templates/{templateId}")
    public ApiResponse<SiteDtos.DeploymentView> deployTemplate(
            @PathVariable Long id,
            @PathVariable String templateId
    ) {
        return ApiResponse.ok(deploymentService.deployTemplate(id, templateId), "模板站点正在发布");
    }

    @PostMapping("/{id}/redeploy")
    public ApiResponse<SiteDtos.DeploymentView> redeploy(@PathVariable Long id) {
        return ApiResponse.ok(deploymentService.redeploy(id), "已开始重新发布");
    }

    @GetMapping("/{id}/deployments")
    public ApiResponse<List<SiteDtos.DeploymentView>> deployments(@PathVariable Long id) {
        return ApiResponse.ok(deploymentService.bySite(id));
    }
}


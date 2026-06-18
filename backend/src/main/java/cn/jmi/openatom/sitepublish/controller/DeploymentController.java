package cn.jmi.openatom.sitepublish.controller;

import cn.jmi.openatom.sitepublish.common.ApiResponse;
import cn.jmi.openatom.sitepublish.dto.SiteDtos;
import cn.jmi.openatom.sitepublish.service.DeploymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/deployments")
@RequiredArgsConstructor
public class DeploymentController {

    private final DeploymentService deploymentService;

    @GetMapping("/recent")
    public ApiResponse<List<SiteDtos.DeploymentView>> recent(@RequestParam(required = false) Integer limit) {
        return ApiResponse.ok(deploymentService.recent(limit));
    }
}


package cn.jmi.openatom.sitepublish.controller;

import cn.jmi.openatom.sitepublish.common.ApiResponse;
import cn.jmi.openatom.sitepublish.dto.AdminDtos;
import cn.jmi.openatom.sitepublish.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/dashboard")
    public ApiResponse<AdminDtos.DashboardView> dashboard() {
        return ApiResponse.ok(adminService.dashboard());
    }

    @PutMapping("/users/{userId}/admin")
    public ApiResponse<AdminDtos.UserView> updateAdminRole(
            @PathVariable Long userId,
            @Valid @RequestBody AdminDtos.UpdateAdminRoleRequest request
    ) {
        return ApiResponse.ok(
                adminService.updateAdminRole(userId, request.enabled()),
                request.enabled() ? "已授予管理员权限" : "已取消管理员权限"
        );
    }

    @DeleteMapping("/sites/{siteId}")
    public ApiResponse<Void> deleteSite(@PathVariable Long siteId) {
        adminService.deleteSite(siteId);
        return ApiResponse.message("项目已删除");
    }
}

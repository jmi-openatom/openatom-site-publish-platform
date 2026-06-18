package cn.jmi.openatom.sitepublish.controller;

import cn.jmi.openatom.sitepublish.common.ApiResponse;
import cn.jmi.openatom.sitepublish.dto.AuthDtos;
import cn.jmi.openatom.sitepublish.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @GetMapping("/config")
    public ApiResponse<AuthDtos.AuthConfig> config() {
        return ApiResponse.ok(authService.config());
    }

    @PostMapping("/oauth/start")
    public ApiResponse<AuthDtos.OAuthStartResponse> start(@Valid @RequestBody AuthDtos.OAuthStartRequest request) {
        return ApiResponse.ok(authService.start(request));
    }

    @PostMapping("/oauth/callback")
    public ApiResponse<AuthDtos.LoginResponse> callback(@Valid @RequestBody AuthDtos.OAuthCallbackRequest request) {
        return ApiResponse.ok(authService.callback(request));
    }

    @PostMapping("/dev-login")
    public ApiResponse<AuthDtos.LoginResponse> devLogin() {
        return ApiResponse.ok(authService.devLogin());
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        authService.logout();
        return ApiResponse.message("已退出登录");
    }
}


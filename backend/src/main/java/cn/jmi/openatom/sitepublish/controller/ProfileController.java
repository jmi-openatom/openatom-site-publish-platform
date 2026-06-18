package cn.jmi.openatom.sitepublish.controller;

import cn.jmi.openatom.sitepublish.common.ApiResponse;
import cn.jmi.openatom.sitepublish.dto.AuthDtos;
import cn.jmi.openatom.sitepublish.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ProfileController {

    private final AuthService authService;

    @GetMapping("/me")
    public ApiResponse<AuthDtos.UserView> me() {
        return ApiResponse.ok(authService.currentUser());
    }
}


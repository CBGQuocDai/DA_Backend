package com.backend.presentation.controller;

import com.backend.application.dto.request.user.LoginRequest;
import com.backend.application.dto.request.user.ChangePasswordRequest;
import com.backend.presentation.dto.ApiResponse;
import com.backend.application.dto.response.user.LoginResponse;
import com.backend.application.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Validated @RequestBody LoginRequest loginRequest) {
        LoginResponse data = adminService.login(loginRequest);
        return ApiResponse.<LoginResponse>builder()
                .data(data)
                .build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/change-password")
    public ApiResponse<Void> changePassword(@AuthenticationPrincipal Long adminId,
                                            @Validated @RequestBody ChangePasswordRequest request) {
        adminService.changePassword(adminId, request);
        return ApiResponse.<Void>builder().message("Đổi mật khẩu thành công").build();
    }
}

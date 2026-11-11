package com.backend.presentation.controller;

import com.backend.application.dto.request.user.ChangePasswordRequest;
import com.backend.application.service.CustomerService;
import com.backend.presentation.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String BEARER_PREFIX = "Bearer ";

    // Logout is role-independent (implemented in AbstractUserService)
    private final CustomerService userService;

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            userService.logout(header.substring(BEARER_PREFIX.length()));
        }
        return ApiResponse.<Void>builder().build();
    }

//    thêm đổi mật khẩu
//    @PostMapping("/change-password")
//    public ApiResponse<Void> changePassword(@AuthenticationPrincipal Long adminId,
//                                            @Validated @RequestBody ChangePasswordRequest request) {
//        adminService.changePassword(adminId, request);
//        return ApiResponse.<Void>builder().message("Đổi mật khẩu thành công").build();
//    }

}

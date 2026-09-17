package com.backend.presentation.controller;

import com.backend.domain.dto.request.user.LoginRequest;
import com.backend.domain.dto.response.ApiResponse;
import com.backend.domain.dto.response.user.LoginResponse;
import com.backend.application.AdminService;
import lombok.RequiredArgsConstructor;
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
}

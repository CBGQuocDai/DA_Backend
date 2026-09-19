package com.backend.presentation.controller;

import com.backend.domain.dto.request.user.LoginRequest;
import com.backend.domain.dto.request.user.RegisterRequest;
import com.backend.domain.dto.response.ApiResponse;
import com.backend.domain.dto.response.user.LoginResponse;
import com.backend.domain.dto.response.user.RegisterResponse;
import com.backend.application.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/customer")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Validated @RequestBody LoginRequest loginRequest) {
        LoginResponse data = customerService.login(loginRequest);
        return ApiResponse.<LoginResponse>builder()
                .data(data)
                .build();
    }

    @PostMapping("/register")
    public ApiResponse<RegisterResponse> register(@Validated @RequestBody RegisterRequest registerRequest) {
        RegisterResponse data = customerService.register(registerRequest);
        return ApiResponse.<RegisterResponse>builder()
                .data(data)
                .build();
    }
}

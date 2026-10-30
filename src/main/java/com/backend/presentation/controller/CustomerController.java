package com.backend.presentation.controller;

import com.backend.application.service.CustomerBookService;
import com.backend.application.service.CustomerService;
import com.backend.domain.dto.request.user.LoginRequest;
import com.backend.domain.dto.request.user.RegisterRequest;
import com.backend.domain.dto.request.user.customer.CustomerChangePasswordRequest;
import com.backend.domain.dto.request.user.customer.CustomerUpdateProfileRequest;
import com.backend.domain.dto.response.ApiResponse;
import com.backend.domain.dto.response.customer.CustomerProfileResponse;
import com.backend.domain.dto.response.user.LoginResponse;
import com.backend.domain.dto.response.user.RegisterResponse;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/customer")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;
    private final CustomerBookService customerBookService;

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Validated @RequestBody LoginRequest loginRequest) {
        LoginResponse data = customerService.login(loginRequest);
        return ApiResponse.<LoginResponse>builder().data(data).build();
    }

    @PostMapping("/register")
    public ApiResponse<RegisterResponse> register(
        @Validated @RequestBody RegisterRequest registerRequest) {
        RegisterResponse data = customerService.register(registerRequest);
        return ApiResponse.<RegisterResponse>builder().data(data).build();
    }

    @GetMapping("/profile")
    public ApiResponse<CustomerProfileResponse> getProfile(@AuthenticationPrincipal Long id) {
        CustomerProfileResponse response = customerService.getProfile(id);
        return ApiResponse.<CustomerProfileResponse>builder().data(response).build();
    }

    @PutMapping("/profile")
    public ApiResponse<CustomerProfileResponse> updateProfile(@AuthenticationPrincipal Long userId,
        @Validated @RequestBody CustomerUpdateProfileRequest request) {
        CustomerProfileResponse response = customerService.updateProfile(userId, request);
        return ApiResponse.<CustomerProfileResponse>builder().data(response).build();
    }

    @PostMapping("/profile/change-password")
    public ApiResponse<Void> changePassword(@AuthenticationPrincipal Long userId,
        @Validated @RequestBody CustomerChangePasswordRequest request) {
        customerService.changePassword(userId, request);
        return ApiResponse.<Void>builder().message("Đổi mật khẩu thành công")
            .build();
    }

    @GetMapping("/books/{bookId}/check")
    public ApiResponse<Map<String, Boolean>> checkBookOwnership(
            @PathVariable Long bookId,
            @AuthenticationPrincipal Long customerId
    ) {
        boolean purchased = customerId != null && customerBookService.hasPurchasedBook(customerId, bookId);
        return ApiResponse.<Map<String, Boolean>>builder()
                .message("Kiểm tra quyền sở hữu sách thành công")
                .data(Map.of("purchased", purchased))
                .build();
    }
}

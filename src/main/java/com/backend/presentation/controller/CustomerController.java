package com.backend.presentation.controller;

import com.backend.application.service.CustomerBookService;
import com.backend.application.service.CustomerService;
import com.backend.application.dto.request.user.LoginRequest;
import com.backend.application.dto.request.user.RegisterRequest;
import com.backend.application.dto.request.user.ChangePasswordRequest;
import com.backend.application.dto.request.user.customer.CustomerUpdateProfileRequest;
import com.backend.presentation.dto.ApiResponse;
import com.backend.application.dto.response.book.BookResponse;
import com.backend.application.dto.response.customer.CustomerProfileResponse;
import com.backend.application.dto.response.user.LoginResponse;
import com.backend.application.dto.response.user.RegisterResponse;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/customer")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;
    private final CustomerBookService customerBookService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Validated @RequestBody LoginRequest loginRequest) {
        LoginResponse data = customerService.login(loginRequest);
        return ResponseEntity.ok(ApiResponse.<LoginResponse>builder().data(data).build());
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisterResponse>> register(
        @Validated @RequestBody RegisterRequest registerRequest) {
        RegisterResponse data = customerService.register(registerRequest);
        return ResponseEntity.ok(ApiResponse.<RegisterResponse>builder().data(data).build());
    }

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<CustomerProfileResponse>> getProfile(@AuthenticationPrincipal Long id) {
        CustomerProfileResponse response = customerService.getProfile(id);
        return ResponseEntity.ok(ApiResponse.<CustomerProfileResponse>builder().data(response).build());
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<CustomerProfileResponse>> updateProfile(@AuthenticationPrincipal Long userId,
        @Validated @RequestBody CustomerUpdateProfileRequest request) {
        CustomerProfileResponse response = customerService.updateProfile(userId, request);
        return ResponseEntity.ok(ApiResponse.<CustomerProfileResponse>builder().data(response).build());
    }

    @PostMapping("/profile/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(@AuthenticationPrincipal Long userId,
        @Validated @RequestBody ChangePasswordRequest request) {
        customerService.changePassword(userId, request);
        return ResponseEntity.ok(ApiResponse.<Void>builder().message("Đổi mật khẩu thành công")
            .build());
    }

    @GetMapping("/books")
    public ResponseEntity<ApiResponse<List<BookResponse>>> getPurchasedBooks(@AuthenticationPrincipal Long customerId) {
        List<BookResponse> data = customerBookService.getPurchasedBooks(customerId);
        return ResponseEntity.ok(ApiResponse.<List<BookResponse>>builder()
                .message("Lấy danh sách sách đã mua thành công")
                .data(data)
                .build());
    }

    @GetMapping("/books/{bookId}/check")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> checkBookOwnership(
            @PathVariable Long bookId,
            @AuthenticationPrincipal Long customerId
    ) {
        boolean purchased = customerBookService.hasPurchasedBook(customerId, bookId);
        return ResponseEntity.ok(ApiResponse.<Map<String, Boolean>>builder()
                .message("Kiểm tra quyền sở hữu sách thành công")
                .data(Map.of("purchased", purchased))
                .build());
    }
}

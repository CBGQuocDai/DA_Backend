package com.backend.presentation.controller;

import com.backend.application.dto.request.user.ChangePasswordRequest;
import com.backend.application.dto.request.user.LoginRequest;
import com.backend.application.dto.response.statistics.PurchaseTrendResponse;
import com.backend.application.dto.response.statistics.StatisticsResponse;
import com.backend.application.dto.response.user.LoginResponse;
import com.backend.application.service.AdminService;
import com.backend.application.service.StatisticsService;
import com.backend.domain.valueobject.BestsellerPeriod;
import com.backend.presentation.dto.ApiResponse;
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
    private final StatisticsService statisticsService;

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

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/statistics")
    public ApiResponse<StatisticsResponse> getStatistics(@RequestParam(defaultValue = "12") int months) {
        return ApiResponse.<StatisticsResponse>builder()
                .data(statisticsService.getStatistics(months))
                .build();
    }

    /** All purchases in the period against the one before; bestseller books and categories have their own endpoints. */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/statistics/purchases")
    public ApiResponse<PurchaseTrendResponse> getPurchaseTrend(
            @RequestParam(defaultValue = "MONTH") BestsellerPeriod period) {
        return ApiResponse.<PurchaseTrendResponse>builder()
                .data(statisticsService.getPurchaseTrend(period))
                .build();
    }
}

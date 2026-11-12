package com.backend.presentation.controller;

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

//    endpoint thống kê doanh thu
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/statistics")
    public ApiResponse<StatisticsResponse> getStatistics(@RequestParam(defaultValue = "12") int months) {
        return ApiResponse.<StatisticsResponse>builder()
                .data(statisticsService.getStatistics(months))
                .build();
    }

//    endpoint thống kê sách bán chạy
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/statistics/purchases")
    public ApiResponse<PurchaseTrendResponse> getPurchaseTrend(
            @RequestParam(defaultValue = "MONTH") BestsellerPeriod period) {
        return ApiResponse.<PurchaseTrendResponse>builder()
                .data(statisticsService.getPurchaseTrend(period))
                .build();
    }
}

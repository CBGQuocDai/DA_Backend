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
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Validated @RequestBody LoginRequest loginRequest) {
        LoginResponse data = adminService.login(loginRequest);
        return ResponseEntity.ok(ApiResponse.<LoginResponse>builder()
                .data(data)
                .build());
    }

//    endpoint thống kê doanh thu
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse<StatisticsResponse>> getStatistics(@RequestParam(defaultValue = "12") int months) {
        return ResponseEntity.ok(ApiResponse.<StatisticsResponse>builder()
                .data(statisticsService.getStatistics(months))
                .build());
    }

//    endpoint thống kê sách bán chạy
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/statistics/purchases")
    public ResponseEntity<ApiResponse<PurchaseTrendResponse>> getPurchaseTrend(
            @RequestParam(defaultValue = "MONTH") BestsellerPeriod period) {
        return ResponseEntity.ok(ApiResponse.<PurchaseTrendResponse>builder()
                .data(statisticsService.getPurchaseTrend(period))
                .build());
    }
}

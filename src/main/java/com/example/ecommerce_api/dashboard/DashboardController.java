package com.example.ecommerce_api.dashboard;

import com.example.ecommerce_api.common.response.ApiResponse;
import com.example.ecommerce_api.dashboard.dto.SalesOverviewResponse;
import com.example.ecommerce_api.dashboard.services.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/dashboard")
@Tag(name = "dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(summary = "Sales figures for the admin dashboard",
            description = "Monthly revenue and paid-order counts for the year (default: current year), "
                    + "this month, last month, today's revenue and order counts per status. "
                    + "Only PAID, PROCESSING, SHIPPED and DELIVERED orders count; revenue is net of refunds.")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/sales-overview")
    public ResponseEntity<ApiResponse<SalesOverviewResponse>> getSalesOverview(
            @RequestParam(required = false) @Min(2000) @Max(2100) Integer year) {


        return ResponseEntity.ok(ApiResponse.success(dashboardService.getSalesOverview(year)));
    }

    }

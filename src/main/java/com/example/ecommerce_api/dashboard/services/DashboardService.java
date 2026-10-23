package com.example.ecommerce_api.dashboard.services;


import com.example.ecommerce_api.dashboard.dto.SalesOverviewResponse;

public interface DashboardService {

    /** @param year the year for the monthly chart; null means the current year */
    SalesOverviewResponse getSalesOverview(Integer year);
}

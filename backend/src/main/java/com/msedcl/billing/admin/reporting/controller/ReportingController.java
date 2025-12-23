package com.msedcl.billing.admin.reporting.controller;

import com.msedcl.billing.admin.reporting.dto.reporting.BillStatusSummaryResponse;
import com.msedcl.billing.admin.reporting.dto.reporting.DashboardMetricsResponse;
import com.msedcl.billing.admin.reporting.dto.reporting.MonthlyAmountResponse;
import com.msedcl.billing.admin.reporting.dto.reporting.MonthlyConsumptionResponse;
import com.msedcl.billing.admin.reporting.service.ReportingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/reports")
@RequiredArgsConstructor
public class ReportingController {

    private final ReportingService reportingService;

    @GetMapping("/dashboard")
    public ResponseEntity<DashboardMetricsResponse> getDashboardMetrics() {
        return ResponseEntity.ok(reportingService.getDashboardMetrics());
    }

    @GetMapping("/collections")
    public ResponseEntity<List<MonthlyAmountResponse>> getCollectionTrend(@RequestParam(defaultValue = "6") int months) {
        return ResponseEntity.ok(reportingService.getCollectionTrend(months));
    }

    @GetMapping("/consumption")
    public ResponseEntity<List<MonthlyConsumptionResponse>> getConsumptionTrend(@RequestParam(defaultValue = "6") int months) {
        return ResponseEntity.ok(reportingService.getConsumptionTrend(months));
    }

    @GetMapping("/bills/status-summary")
    public ResponseEntity<BillStatusSummaryResponse> getBillStatusSummary() {
        return ResponseEntity.ok(reportingService.getBillStatusSummary());
    }
}

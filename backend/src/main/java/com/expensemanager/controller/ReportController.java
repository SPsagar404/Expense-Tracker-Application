package com.expensemanager.controller;

import com.expensemanager.dto.ApiResponse;
import com.expensemanager.dto.ReportDto;
import com.expensemanager.service.ReportService;
import com.expensemanager.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@Tag(name = "Reports", description = "Monthly spending reports and analytics")
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/monthly")
    @Operation(summary = "Get monthly spending report with category breakdown and trends")
    public ResponseEntity<ApiResponse<ReportDto>> getMonthlyReport(
            @RequestParam int year,
            @RequestParam int month) {
        Long userId = SecurityUtils.getCurrentUserId();
        ReportDto report = reportService.getMonthlyReport(userId, year, month);
        return ResponseEntity.ok(ApiResponse.success(report));
    }
}

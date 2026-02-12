package com.expensemanager.controller;

import com.expensemanager.dto.ApiResponse;
import com.expensemanager.dto.InsightDto;
import com.expensemanager.service.InsightService;
import com.expensemanager.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/insights")
@RequiredArgsConstructor
@Tag(name = "Insights", description = "AI-ready spending insights and anomaly detection")
public class InsightController {

    private final InsightService insightService;

    @GetMapping("/summary")
    @Operation(summary = "Get rule-based spending insights and flags")
    public ResponseEntity<ApiResponse<InsightDto>> getSummary() {
        Long userId = SecurityUtils.getCurrentUserId();
        InsightDto insights = insightService.getSummary(userId);
        return ResponseEntity.ok(ApiResponse.success(insights));
    }
}

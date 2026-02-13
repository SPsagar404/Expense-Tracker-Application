package com.expensemanager.controller;

import com.expensemanager.dto.ApiResponse;
import com.expensemanager.dto.SubscriptionDto;
import com.expensemanager.dto.SubscriptionSummaryDto;
import com.expensemanager.dto.WasteAnalysisDto;
import com.expensemanager.service.SubscriptionService;
import com.expensemanager.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/subscriptions")
@RequiredArgsConstructor
@Tag(name = "Subscriptions", description = "Subscription management and auto-billing")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @PostMapping
    @Operation(summary = "Create a new subscription")
    public ResponseEntity<ApiResponse<SubscriptionDto>> create(
            @Valid @RequestBody SubscriptionDto dto) {
        Long userId = SecurityUtils.getCurrentUserId();
        SubscriptionDto result = subscriptionService.createSubscription(userId, dto);
        return ResponseEntity.ok(ApiResponse.success("Subscription created", result));
    }

    @GetMapping
    @Operation(summary = "List all user subscriptions")
    public ResponseEntity<ApiResponse<List<SubscriptionDto>>> list() {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(subscriptionService.getUserSubscriptions(userId)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a subscription")
    public ResponseEntity<ApiResponse<SubscriptionDto>> update(
            @PathVariable Long id,
            @Valid @RequestBody SubscriptionDto dto) {
        Long userId = SecurityUtils.getCurrentUserId();
        SubscriptionDto result = subscriptionService.updateSubscription(userId, id, dto);
        return ResponseEntity.ok(ApiResponse.success("Subscription updated", result));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a subscription")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        subscriptionService.deleteSubscription(userId, id);
        return ResponseEntity.ok(ApiResponse.success("Subscription deleted", null));
    }

    @GetMapping("/summary")
    @Operation(summary = "Get subscription summary with monthly/yearly totals and upcoming billing")
    public ResponseEntity<ApiResponse<SubscriptionSummaryDto>> getSummary() {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(subscriptionService.getSummary(userId)));
    }

    @GetMapping("/waste-analysis")
    @Operation(summary = "Analyze subscription waste: duplicates, high burden, unused")
    public ResponseEntity<ApiResponse<WasteAnalysisDto>> wasteAnalysis() {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(subscriptionService.analyzeWaste(userId)));
    }
}

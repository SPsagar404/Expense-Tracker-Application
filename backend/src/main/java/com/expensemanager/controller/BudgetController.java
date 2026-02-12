package com.expensemanager.controller;

import com.expensemanager.dto.ApiResponse;
import com.expensemanager.dto.BudgetDto;
import com.expensemanager.service.BudgetService;
import com.expensemanager.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/budgets")
@RequiredArgsConstructor
@Tag(name = "Budgets", description = "Budget management with utilization tracking")
public class BudgetController {

    private final BudgetService budgetService;

    @GetMapping
    @Operation(summary = "List budgets, optionally filtered by month and year")
    public ResponseEntity<ApiResponse<List<BudgetDto>>> getBudgets(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year) {
        Long userId = SecurityUtils.getCurrentUserId();
        List<BudgetDto> budgets = budgetService.getBudgets(userId, month, year);
        return ResponseEntity.ok(ApiResponse.success(budgets));
    }

    @PostMapping
    @Operation(summary = "Create a new budget")
    public ResponseEntity<ApiResponse<BudgetDto>> createBudget(
            @Valid @RequestBody BudgetDto dto) {
        Long userId = SecurityUtils.getCurrentUserId();
        BudgetDto created = budgetService.createBudget(userId, dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Budget created", created));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing budget")
    public ResponseEntity<ApiResponse<BudgetDto>> updateBudget(
            @PathVariable Long id,
            @Valid @RequestBody BudgetDto dto) {
        Long userId = SecurityUtils.getCurrentUserId();
        BudgetDto updated = budgetService.updateBudget(userId, id, dto);
        return ResponseEntity.ok(ApiResponse.success("Budget updated", updated));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a budget")
    public ResponseEntity<ApiResponse<Void>> deleteBudget(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        budgetService.deleteBudget(userId, id);
        return ResponseEntity.ok(ApiResponse.success("Budget deleted", null));
    }
}

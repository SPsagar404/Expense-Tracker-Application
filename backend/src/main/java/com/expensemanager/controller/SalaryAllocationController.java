package com.expensemanager.controller;

import com.expensemanager.dto.ApiResponse;
import com.expensemanager.dto.SalaryAllocationDto;
import com.expensemanager.dto.SalarySummaryDto;
import com.expensemanager.service.SalaryAllocationService;
import com.expensemanager.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Salary Allocation", description = "Monthly salary planning and allocation management")
public class SalaryAllocationController {

    private final SalaryAllocationService salaryService;

    @PostMapping("/salary")
    @Operation(summary = "Create or replace monthly salary plan with allocations")
    public ResponseEntity<ApiResponse<List<SalaryAllocationDto>>> saveSalaryPlan(
            @Valid @RequestBody List<SalaryAllocationDto> dtos) {
        Long userId = SecurityUtils.getCurrentUserId();
        List<SalaryAllocationDto> result = salaryService.saveSalaryPlan(userId, dtos);
        return ResponseEntity.ok(ApiResponse.success("Salary plan saved", result));
    }

    @PostMapping("/salary/{id}/allocate")
    @Operation(summary = "Add a single allocation to an existing salary plan")
    public ResponseEntity<ApiResponse<SalaryAllocationDto>> addAllocation(
            @PathVariable Long id,
            @Valid @RequestBody SalaryAllocationDto dto) {
        Long userId = SecurityUtils.getCurrentUserId();
        SalaryAllocationDto result = salaryService.addAllocation(userId, id, dto);
        return ResponseEntity.ok(ApiResponse.success("Allocation added", result));
    }

    @GetMapping("/salary/{year}/{month}")
    @Operation(summary = "Get salary allocations for a specific month")
    public ResponseEntity<ApiResponse<List<SalaryAllocationDto>>> getAllocations(
            @PathVariable Integer year,
            @PathVariable Integer month) {
        Long userId = SecurityUtils.getCurrentUserId();
        List<SalaryAllocationDto> result = salaryService.getAllocations(userId, year, month);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/reports/salary-summary")
    @Operation(summary = "Get salary summary with planned vs actual spending per category")
    public ResponseEntity<ApiResponse<SalarySummaryDto>> getSalarySummary(
            @RequestParam Integer year,
            @RequestParam Integer month) {
        Long userId = SecurityUtils.getCurrentUserId();
        SalarySummaryDto summary = salaryService.getSalarySummary(userId, year, month);
        return ResponseEntity.ok(ApiResponse.success(summary));
    }
}

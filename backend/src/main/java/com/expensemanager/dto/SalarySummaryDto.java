package com.expensemanager.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalarySummaryDto {
    private Integer month;
    private Integer year;
    private BigDecimal totalSalary;
    private BigDecimal totalPlanned;
    private BigDecimal totalActualSpent;
    private BigDecimal totalSavings;
    private BigDecimal unallocatedAmount;
    private List<CategoryAllocation> allocations;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryAllocation {
        private Long id;
        private String category;
        private String allocationType;
        private BigDecimal allocationValue;
        private BigDecimal allocatedAmount;
        private BigDecimal actualSpent;
        private BigDecimal variance; // allocated - actual (positive = under budget)
    }
}

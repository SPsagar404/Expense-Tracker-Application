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
public class InsightDto {
    private List<InsightItem> insights;
    private int totalFlags;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InsightItem {
        private String type; // HIGH_EXPENSE, DUPLICATE_SUBSCRIPTION
        private String severity; // WARNING, INFO
        private String message;
        private String category;
        private BigDecimal amount;
    }
}

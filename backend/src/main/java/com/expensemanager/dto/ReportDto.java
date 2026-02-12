package com.expensemanager.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportDto implements Serializable {
    private Integer month;
    private Integer year;
    private BigDecimal totalSpent;
    private List<CategoryBreakdown> categoryBreakdown;
    private List<DailyTrend> dailyTrends;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryBreakdown implements Serializable {
        private String category;
        private BigDecimal amount;
        private Double percentage;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyTrend implements Serializable {
        private String date;
        private BigDecimal amount;
    }
}

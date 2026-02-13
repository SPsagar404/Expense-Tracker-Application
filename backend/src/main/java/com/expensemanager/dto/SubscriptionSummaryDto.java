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
public class SubscriptionSummaryDto {
    private BigDecimal totalMonthlyCommitment;
    private BigDecimal totalYearlyCommitment;
    private long activeSubscriptions;
    private List<SubscriptionDto> upcomingIn7Days;
}

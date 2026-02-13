package com.expensemanager.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalaryAllocationDto {
    private Long id;

    @NotNull(message = "Month is required")
    @Min(value = 1, message = "Month must be between 1 and 12")
    @Max(value = 12, message = "Month must be between 1 and 12")
    private Integer month;

    @NotNull(message = "Year is required")
    @Min(value = 2020, message = "Year must be 2020 or later")
    private Integer year;

    @NotNull(message = "Total salary is required")
    @Positive(message = "Total salary must be positive")
    private BigDecimal totalSalary;

    @NotBlank(message = "Category is required")
    private String category;

    @NotBlank(message = "Allocation type is required (PERCENTAGE or FIXED)")
    private String allocationType;

    @NotNull(message = "Allocation value is required")
    @PositiveOrZero(message = "Allocation value must be zero or positive")
    private BigDecimal allocationValue;

    private BigDecimal allocatedAmount;
}

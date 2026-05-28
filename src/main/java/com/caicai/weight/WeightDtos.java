package com.caicai.weight;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class WeightDtos {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateWeightRequest {

        @NotNull(message = "Weight is required")
        @DecimalMin(value = "1.0", message = "Weight must be at least 1kg")
        private BigDecimal weightKg;

        @NotNull(message = "Date is required")
        private LocalDate date;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WeightLogResponse {
        private Long id;
        private BigDecimal weightKg;
        private LocalDateTime loggedAt;
    }
}
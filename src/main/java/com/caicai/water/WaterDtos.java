package com.caicai.water;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class WaterDtos {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateWaterRequest {
        @NotNull(message = "Amount is required")
        @Min(value = 1, message = "Amount must be at least 1ml")
        private Integer amountMl;

        @NotNull(message = "Date is required")
        private LocalDate date;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WaterLogResponse {
        private Long id;
        private Integer amountMl;
        private LocalDateTime loggedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WaterSummaryResponse {
        private Integer totalMl;
        private java.util.List<WaterLogResponse> entries;
    }
}
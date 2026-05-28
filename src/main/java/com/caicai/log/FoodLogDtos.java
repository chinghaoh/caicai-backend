package com.caicai.log;

import com.caicai.log.FoodLog.MealType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class FoodLogDtos {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateFoodLogRequest {

        @NotNull(message = "Food item is required")
        private Long foodItemId;

        @NotNull(message = "Amount is required")
        @Min(value = 1, message = "Amount must be at least 1g")
        private Integer amountGrams;

        @NotNull(message = "Meal type is required")
        private MealType mealType;

        @NotNull(message = "Date is required")
        private LocalDate date;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FoodLogResponse {
        private Long id;
        private Long foodItemId;
        private String foodName;
        private String brand;
        private Integer amountGrams;
        private MealType mealType;
        private LocalDateTime loggedAt;

        private Double calories;
        private Double protein;
        private Double carbs;
        private Double fat;
    }
}
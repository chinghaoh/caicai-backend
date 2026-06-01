package com.caicai.goal;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public class GoalDtos {

    public record SuggestRequest(
            @NotNull(message = "Age is required")
            @Min(value = 10, message = "Age must be at least 10")
            @Max(value = 120, message = "Age must be at most 120")
            Integer age,

            @NotNull(message = "Weight is required")
            @DecimalMin(value = "20.0", message = "Weight must be at least 20 kg")
            @DecimalMax(value = "300.0", message = "Weight must be at most 300kg")
            BigDecimal weightKg,

            @NotNull(message = "Height is required")
            @Min(value = 50, message = "Height must be at least 50 cm")
            @Max(value = 220, message = "Height must be at most 220cm")
            Integer heightCm,

            @NotBlank(message = "Gender is required")
            String gender,

            @NotNull(message = "Activity level is required")
            ActivityLevel activityLevel,

            @NotNull(message = "Goal type is required")
            GoalType goalType,

            @NotNull(message = "Target weight is required")
            @DecimalMin(value = "20.0", message = "Target weight must be at least 20 kg")
            @DecimalMax(value = "300.0", message = "Target Weight can be at most 300kg")
            BigDecimal targetWeightKg
    ) {}

    public record CreateGoalRequest(
            @NotNull(message = "Calories is required")
            @Min(value = 500, message = "Calories must be at least 500")
            @Max(value = 10000, message = "Calories must be at most 10000")
            Integer calories,

            @NotNull(message = "Protein is required")
            @Min(value = 0, message = "Protein must be non-negative")
            Integer protein,

            @NotNull(message = "Carbs is required")
            @Min(value = 0, message = "Carbs must be non-negative")
            Integer carbs,

            @NotNull(message = "Fat is required")
            @Min(value = 0, message = "Fat must be non-negative")
            Integer fat,

            @NotNull(message = "Water goal is required")
            @Min(value = 500, message = "Water goal must be at least 500 ml")
            Integer waterMl,

            @NotNull(message = "Starting weight is required")
            @DecimalMin(value = "20.0", message = "Starting weight must be at least 20 kg")
            BigDecimal startingWeightKg,

            @NotNull(message = "Target weight is required")
            @DecimalMin(value = "20.0", message = "Target weight must be at least 20 kg")
            BigDecimal targetWeightKg
    ) {}

    public record SuggestResponse(
            int calories,
            int protein,
            int carbs,
            int fat,
            int waterMl,
            String explanation
    ) {}

    public record GoalResponse(
            Long id,
            int calories,
            int protein,
            int carbs,
            int fat,
            int waterMl,
            BigDecimal startingWeightKg,
            BigDecimal targetWeightKg,
            String effectiveFrom
    ) {}

    public enum ActivityLevel {
        SEDENTARY, LIGHT, MODERATE, ACTIVE, VERY_ACTIVE
    }

    public enum GoalType {
        LOSE_WEIGHT, MAINTAIN, GAIN_MUSCLE
    }
}
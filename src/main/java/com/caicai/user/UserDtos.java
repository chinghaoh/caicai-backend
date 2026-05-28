package com.caicai.user;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public class UserDtos {

    public record UserResponse(
            Long id,
            String email,
            String name,
            boolean isVerified,
            boolean isDemo,
            boolean hasCompletedOnboarding,
            Integer age,
            BigDecimal weightKg,
            BigDecimal  heightCm,
            String gender,
            String activityLevel
    ) {}

    public record UpdateProfileRequest(
            @NotBlank(message = "Name is required")
            String name,

            @NotNull(message = "Age is required")
            @Min(value = 10, message = "Age must be at least 10")
            @Max(value = 120, message = "Age must be at most 120")
            Integer age,

            @NotNull(message = "Weight is required")
            @DecimalMin(value = "20.0", message = "Weight must be at least 20kg")
            @DecimalMax(value = "500.0", message = "Weight must be at most 500kg")
            BigDecimal weightKg,

            @NotNull(message = "Height is required")
            @Min(value = 50, message = "Height must be at least 50cm")
            @Max(value = 300, message = "Height must be at most 300cm")
            BigDecimal heightCm,

            @NotNull(message = "Gender is required")
            User.Gender gender,

            @NotNull(message = "Activity level is required")
            User.ActivityLevel activityLevel
    ) {}
}
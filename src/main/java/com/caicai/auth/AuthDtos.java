package com.caicai.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class AuthDtos {

    public record RegisterRequest(
            @NotBlank(message = "Email is required")
            @Email(message = "Email is invalid")
            String email,

            @NotBlank(message = "Password is required")
            @Pattern(
                    regexp = "^(?=.*[0-9])(?=.*[A-Z]).{6,}$",
                    message = "Password must be at least 6 characters, contain one uppercase letter and one digit"
            )
            String password,

            @NotBlank(message = "Name is required")
            String name
    ) {}

    public record LoginRequest(
            @NotBlank(message = "Email is required")
            @Email(message = "Email is invalid")
            String email,

            @NotBlank(message = "Password is required")
            String password
    ) {}

    public record ForgotPasswordRequest(
            @NotBlank(message = "Email is required")
            @Email(message = "Email is invalid")
            String email
    ) {}

    public record ResetPasswordRequest(
            @NotBlank(message = "Token is required")
            String token,

            @NotBlank(message = "Password is required")
            @Pattern(
                    regexp = "^(?=.*[0-9])(?=.*[A-Z]).{6,}$",
                    message = "Password must be at least 6 characters, contain one uppercase letter and one digit"
            )
            String password
    ) {}

    public record AuthResponse(
            Long id,
            String email,
            String name,
            boolean isVerified,
            boolean isDemo,
            boolean hasCompletedOnboarding
    ) {}
}
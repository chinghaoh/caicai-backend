package com.caicai.user;

public class UserDtos {

    public record UserResponse(
            Long id,
            String email,
            String name,
            boolean isVerified,
            boolean isDemo,
            boolean hasCompletedOnboarding
    ) {}
}
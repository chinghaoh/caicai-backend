package com.caicai.user;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    public UserDtos.UserResponse getMe(User user) {
        return toUserResponse(user);
    }

    private UserDtos.UserResponse toUserResponse(User user) {
        return new UserDtos.UserResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.isVerified(),
                user.isDemo(),
                user.isHasCompletedOnboarding()
        );
    }
}
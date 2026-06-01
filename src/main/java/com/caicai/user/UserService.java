package com.caicai.user;

import com.caicai.common.AppException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    public UserDtos.UserResponse getMe(User user) {
        return toUserResponse(user);
    }

    @Transactional
    public UserDtos.UserResponse updateProfile(User user, UserDtos.UpdateProfileRequest dto) {
        if (dto.name() != null)          user.setName(dto.name());
        if (dto.age() != null)           user.setAge(dto.age());
        if (dto.weightKg() != null)      user.setWeightKg(dto.weightKg());
        if (dto.heightCm() != null)      user.setHeightCm(dto.heightCm());
        if (dto.gender() != null)        user.setGender(dto.gender());
        if (dto.activityLevel() != null) user.setActivityLevel(dto.activityLevel());
        userRepository.save(user);
        return toUserResponse(user);
    }

    @Transactional
    public void deleteAccount(User user) {
        userRepository.delete(user);
    }

    @Transactional
    public void completeOnboarding(User user) {
        user.setHasCompletedOnboarding(true);
        userRepository.save(user);
    }

    private UserDtos.UserResponse toUserResponse(User user) {
        return new UserDtos.UserResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.isVerified(),
                user.isDemo(),
                user.isHasCompletedOnboarding(),
                user.getAge(),
                user.getWeightKg(),
                user.getHeightCm(),
                user.getGender() != null ? user.getGender().name() : null,
                user.getActivityLevel() != null ? user.getActivityLevel().name() : null
        );
    }

    public Long getIdByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "User not found"))
                .getId();
    }
}
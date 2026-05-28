package com.caicai.user;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getMe(@AuthenticationPrincipal User user) {
        UserDtos.UserResponse response = userService.getMe(user);
        return ResponseEntity.ok(Map.of("data", response));
    }

    @PostMapping("/me/complete-onboarding")
    public ResponseEntity<Map<String, String>> completeOnboarding(@AuthenticationPrincipal User user) {
        userService.completeOnboarding(user);
        return ResponseEntity.ok(Map.of("message", "Onboarding completed"));
    }

    @PutMapping("/me")
    public ResponseEntity<Map<String, Object>> updateProfile(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody UserDtos.UpdateProfileRequest dto) {
        UserDtos.UserResponse response = userService.updateProfile(user, dto);
        return ResponseEntity.ok(Map.of("data", response));
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteAccount(@AuthenticationPrincipal User user) {
        userService.deleteAccount(user);
        return ResponseEntity.noContent().build();
    }
}
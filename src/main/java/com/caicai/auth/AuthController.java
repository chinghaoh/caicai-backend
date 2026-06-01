package com.caicai.auth;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@Valid @RequestBody AuthDtos.RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.ok(Map.of("message", "Registration successful. Please check your email to verify your account."));
    }

    @GetMapping("/verify")
    public RedirectView verify(
            @RequestParam String token,
            HttpServletResponse response) {

        authService.verify(token, response);

        return new RedirectView("http://localhost:5173/login");
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody AuthDtos.LoginRequest request,
                                                     HttpServletResponse response) {
        AuthDtos.AuthResponse authResponse = authService.login(request, response);
        return ResponseEntity.ok(Map.of("data", authResponse));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(HttpServletResponse response) {
        authService.logout(response);
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody AuthDtos.ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(Map.of("message", "If an account exists with this email, you will receive a password reset link."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody AuthDtos.ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(Map.of("message", "Password reset successfully. Please log in with your new password."));
    }

    @PostMapping("/demo")
    public ResponseEntity<Map<String, Object>> demo(HttpServletResponse response) {
        AuthDtos.AuthResponse authResponse = authService.demo(response);
        return ResponseEntity.ok(Map.of("data", authResponse));
    }
}
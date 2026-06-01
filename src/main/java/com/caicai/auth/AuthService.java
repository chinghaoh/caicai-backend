package com.caicai.auth;

import com.caicai.common.AppException;
import com.caicai.email.EmailService;
import com.caicai.user.User;
import com.caicai.user.UserRepository;
import com.caicai.config.JwtUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final VerificationTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    @Value("${app.cookie.secure}")
    private boolean cookieSecure;

    @Transactional
    public void register(AuthDtos.RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new AppException(HttpStatus.CONFLICT, "An account with this email already exists");
        }

        User user = User.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .name(request.name())
                .verified(false)
                .demo(false)
                .build();

        userRepository.save(user);

        String token = generateAndSaveToken(user, VerificationToken.TokenType.EMAIL_VERIFICATION, 24);
        emailService.sendVerificationEmail(user.getEmail(), token);
    }

    @Transactional
    public AuthDtos.AuthResponse verify(String token, HttpServletResponse response) {
        VerificationToken verificationToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new AppException(HttpStatus.BAD_REQUEST, "Invalid verification token"));

        if (verificationToken.isExpired()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Verification token has expired");
        }

        if (verificationToken.isUsed()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Verification token has already been used");
        }

        if (verificationToken.getType() != VerificationToken.TokenType.EMAIL_VERIFICATION) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Invalid token type");
        }

        User user = verificationToken.getUser();
        user.setVerified(true);
        userRepository.save(user);

        verificationToken.setUsedAt(LocalDateTime.now());
        tokenRepository.save(verificationToken);

        return toAuthResponse(user);
    }

    @Transactional
    public AuthDtos.AuthResponse login(AuthDtos.LoginRequest request, HttpServletResponse response) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new AppException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));

        if (!user.isVerified()) {
            throw new AppException(HttpStatus.UNAUTHORIZED, "Please verify your email before logging in");
        }

        setJwtCookie(response, jwtUtil.generateToken(user.getId()));
        return toAuthResponse(user);
    }

    public void logout(HttpServletResponse response) {
        Cookie cookie = new Cookie("jwt", "");
        cookie.setHttpOnly(true);
        cookie.setSecure(cookieSecure);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    @Transactional
    public void forgotPassword(AuthDtos.ForgotPasswordRequest request) {
        userRepository.findByEmail(request.email()).ifPresent(user -> {
            String token = generateAndSaveToken(user, VerificationToken.TokenType.PASSWORD_RESET, 1);
            emailService.sendPasswordResetEmail(user.getEmail(), token);
        });
    }

    @Transactional
    public void resetPassword(AuthDtos.ResetPasswordRequest request) {
        VerificationToken verificationToken = tokenRepository.findByToken(request.token())
                .orElseThrow(() -> new AppException(HttpStatus.BAD_REQUEST, "Invalid reset token"));

        if (verificationToken.isExpired()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Reset token has expired");
        }

        if (verificationToken.isUsed()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Reset token has already been used");
        }

        if (verificationToken.getType() != VerificationToken.TokenType.PASSWORD_RESET) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Invalid token type");
        }

        User user = verificationToken.getUser();
        user.setPassword(passwordEncoder.encode(request.password()));
        userRepository.save(user);

        verificationToken.setUsedAt(LocalDateTime.now());
        tokenRepository.save(verificationToken);
    }

    @Transactional
    public AuthDtos.AuthResponse demo(HttpServletResponse response) {
        String demoEmail = "demo_" + UUID.randomUUID() + "@caicai.demo";

        User user = User.builder()
                .email(demoEmail)
                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                .name("Demo User")
                .verified(true)
                .demo(true)
                .hasCompletedOnboarding(false)
                .build();

        userRepository.save(user);

        setJwtCookie(response, jwtUtil.generateToken(user.getId()));
        return toAuthResponse(user);
    }

    private String generateAndSaveToken(User user, VerificationToken.TokenType type, int expiryHours) {
        String token = UUID.randomUUID().toString();
        VerificationToken verificationToken = VerificationToken.builder()
                .user(user)
                .token(token)
                .type(type)
                .expiresAt(LocalDateTime.now().plusHours(expiryHours))
                .build();
        tokenRepository.save(verificationToken);
        return token;
    }

    private void setJwtCookie(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie("jwt", token);
        cookie.setHttpOnly(true);
        cookie.setSecure(cookieSecure);
        cookie.setPath("/");
        cookie.setMaxAge(86400);
        response.addCookie(cookie);
    }

    private AuthDtos.AuthResponse toAuthResponse(User user) {
        return new AuthDtos.AuthResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.isVerified(),
                user.isDemo(),
                user.isHasCompletedOnboarding()
        );
    }
}
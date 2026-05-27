package com.caicai.user;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String name;

    private Integer age;

    @Column(precision = 5, scale = 2)
    private BigDecimal weightKg;

    @Column(precision = 5, scale = 2)
    private BigDecimal heightCm;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    @Enumerated(EnumType.STRING)
    private ActivityLevel activityLevel;

    @Column(nullable = false)
    private boolean isVerified = false;

    @Column(nullable = false)
    private boolean isDemo = false;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public enum Gender {
        MALE, FEMALE, OTHER
    }

    public enum ActivityLevel {
        SEDENTARY ,LIGHT, MODERATE, ACTIVE, VERY_ACTIVE
    }
}
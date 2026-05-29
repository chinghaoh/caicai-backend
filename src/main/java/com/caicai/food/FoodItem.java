package com.caicai.food;

import com.caicai.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "food_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FoodItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String brand;

    @Column(name = "calories_per_100g", nullable = false, precision = 7, scale = 2)
    private BigDecimal caloriesPer100g;

    @Column(name = "protein_per_100g", nullable = false, precision = 7, scale = 2)
    private BigDecimal proteinPer100g;

    @Column(name = "carbs_per_100g", nullable = false, precision = 7, scale = 2)
    private BigDecimal carbsPer100g;

    @Column(name = "fat_per_100g", nullable = false, precision = 7, scale = 2)
    private BigDecimal fatPer100g;

    @Column(name = "fiber_per_100g", precision = 7, scale = 2)
    private BigDecimal fiberPer100g;

    @Column(name = "sugar_per_100g", precision = 7, scale = 2)
    private BigDecimal sugarPer100g;

    @Column(name = "sodium_per_100g", precision = 7, scale = 2)
    private BigDecimal sodiumPer100g;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Source source;

    private String externalId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public enum Source {
        FATSECRET, USER_CREATED
    }
}
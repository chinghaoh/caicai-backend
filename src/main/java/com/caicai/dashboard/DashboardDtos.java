package com.caicai.dashboard;

import com.caicai.log.FoodLogDtos.FoodLogResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class DashboardDtos {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailySummary {
        private String date;
        private Totals totals;
        private GoalSnapshot goal;
        private Progress progress;
        private WeightSnapshot weight;
        private Map<String, List<FoodLogResponse>> logsByMealType;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Totals {
        private double calories;
        private double protein;
        private double carbs;
        private double fat;
        private int waterMl;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GoalSnapshot {
        private int calories;
        private int protein;
        private int carbs;
        private int fat;
        private int waterMl;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Progress {
        private int calories;
        private int protein;
        private int carbs;
        private int fat;
        private int waterMl;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WeightSnapshot {
        private BigDecimal current;
        private BigDecimal starting;
        private BigDecimal target;
        private Integer progress;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyDataPoint {
        private String date;
        private double calories;
        private double protein;
        private double carbs;
        private double fat;
        private int waterMl;
        private BigDecimal weightKg;
    }
}
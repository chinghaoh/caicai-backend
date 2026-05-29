package com.caicai.dashboard;

import com.caicai.goal.Goal;
import com.caicai.goal.GoalRepository;
import com.caicai.log.FoodLog;
import com.caicai.log.FoodLogRepository;
import com.caicai.log.FoodLogDtos.FoodLogResponse;
import com.caicai.log.FoodLogService;
import com.caicai.water.WaterLog;
import com.caicai.water.WaterLogRepository;
import com.caicai.weight.WeightLog;
import com.caicai.weight.WeightLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final FoodLogRepository foodLogRepository;
    private final FoodLogService foodLogService;
    private final WaterLogRepository waterLogRepository;
    private final WeightLogRepository weightLogRepository;
    private final GoalRepository goalRepository;

    public DashboardDtos.DailySummary getDailySummary(Long userId, LocalDate date) {
        // Food entries
        log.info("getDailySummary called — userId={}, date={}", userId, date);
        List<FoodLogResponse> entries = List.of();
        try {
            entries = foodLogService.getByDate(userId, date);
        } catch (Exception e) {
            log.warn("Failed to load food entries for user {} on {}: {}", userId, date, e.getMessage());
        }

        // Totals
        double calories = entries.stream().mapToDouble(FoodLogResponse::getCalories).sum();
        double protein  = entries.stream().mapToDouble(FoodLogResponse::getProtein).sum();
        double carbs    = entries.stream().mapToDouble(FoodLogResponse::getCarbs).sum();
        double fat      = entries.stream().mapToDouble(FoodLogResponse::getFat).sum();

        // Water
        int waterMl = 0;
        try {
            waterMl = waterLogRepository.findByUserIdAndDate(userId, date)
                    .stream().mapToInt(WaterLog::getAmountMl).sum();
        } catch (Exception e) {
            log.warn("Failed to load water entries for user {} on {}: {}", userId, date, e.getMessage());
        }

        // Goal
        Optional<Goal> goalOpt = Optional.empty();
        try {
            goalOpt = goalRepository.findActiveGoalForDate(userId, date);
        } catch (Exception e) {
            log.warn("Failed to load goal for user {} on {}: {}", userId, date, e.getMessage());
        }

        DashboardDtos.GoalSnapshot goalSnapshot = goalOpt.map(g -> DashboardDtos.GoalSnapshot.builder()
                .calories(g.getCalories())
                .protein(g.getProtein())
                .carbs(g.getCarbs())
                .fat(g.getFat())
                .waterMl(g.getWaterMl())
                .build()).orElse(null);

        // Progress
        DashboardDtos.Progress progress = null;
        if (goalSnapshot != null) {
            progress = DashboardDtos.Progress.builder()
                    .calories(toPercent(calories, goalSnapshot.getCalories()))
                    .protein(toPercent(protein, goalSnapshot.getProtein()))
                    .carbs(toPercent(carbs, goalSnapshot.getCarbs()))
                    .fat(toPercent(fat, goalSnapshot.getFat()))
                    .waterMl(toPercent(waterMl, goalSnapshot.getWaterMl()))
                    .build();
        }

        // Weight
        DashboardDtos.WeightSnapshot weightSnapshot = null;
        try {
            weightSnapshot = buildWeightSnapshot(userId, goalOpt.orElse(null));
        } catch (Exception e) {
            log.warn("Failed to build weight snapshot for user {}: {}", userId, e.getMessage());
        }

        // Group by meal type
        Map<String, List<FoodLogResponse>> byMeal = entries.stream()
                .collect(Collectors.groupingBy(e -> e.getMealType().name()));

        return DashboardDtos.DailySummary.builder()
                .date(date.toString())
                .totals(DashboardDtos.Totals.builder()
                        .calories(round(calories))
                        .protein(round(protein))
                        .carbs(round(carbs))
                        .fat(round(fat))
                        .waterMl(waterMl)
                        .build())
                .goal(goalSnapshot)
                .progress(progress)
                .weight(weightSnapshot)
                .logsByMealType(byMeal)
                .build();
    }

    public List<DashboardDtos.DailyDataPoint> getWeeklySummary(Long userId, LocalDate date) {
        LocalDate from = date.minusDays(6);
        return buildDataPoints(userId, from, date);
    }

    public List<DashboardDtos.DailyDataPoint> getMonthlySummary(Long userId, LocalDate date) {
        LocalDate from = date.minusDays(29);
        return buildDataPoints(userId, from, date);
    }

    private List<DashboardDtos.DailyDataPoint> buildDataPoints(Long userId, LocalDate from, LocalDate to) {
        List<FoodLog> foodLogs = List.of();
        List<WaterLog> waterLogs = List.of();
        List<WeightLog> weightLogs = List.of();

        try {
            foodLogs = foodLogRepository.findByUserIdAndDateBetween(userId, from, to);
        } catch (Exception e) {
            log.warn("Failed to load food logs for range {} to {}: {}", from, to, e.getMessage());
        }
        try {
            waterLogs = waterLogRepository.findByUserIdAndDateBetween(userId, from, to);
        } catch (Exception e) {
            log.warn("Failed to load water logs for range {} to {}: {}", from, to, e.getMessage());
        }
        try {
            weightLogs = weightLogRepository.findByUserIdAndDateBetween(userId, from, to);
        } catch (Exception e) {
            log.warn("Failed to load weight logs for range {} to {}: {}", from, to, e.getMessage());
        }

        Map<LocalDate, List<FoodLog>> foodByDate = foodLogs.stream()
                .collect(Collectors.groupingBy(f -> f.getLoggedAt().toLocalDate()));
        Map<LocalDate, List<WaterLog>> waterByDate = waterLogs.stream()
                .collect(Collectors.groupingBy(w -> w.getLoggedAt().toLocalDate()));
        Map<LocalDate, WeightLog> weightByDate = weightLogs.stream()
                .collect(Collectors.toMap(
                        w -> w.getLoggedAt().toLocalDate(),
                        w -> w,
                        (a, b) -> b
                ));

        List<DashboardDtos.DailyDataPoint> points = new ArrayList<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            List<FoodLog> dayFood = foodByDate.getOrDefault(d, List.of());
            List<WaterLog> dayWater = waterByDate.getOrDefault(d, List.of());
            WeightLog dayWeight = weightByDate.get(d);

            double calories = dayFood.stream()
                    .filter(f -> f.getFoodItem() != null)
                    .mapToDouble(f -> f.getFoodItem().getCaloriesPer100g().doubleValue() * f.getAmountGrams() / 100.0)
                    .sum();
            double protein = dayFood.stream()
                    .filter(f -> f.getFoodItem() != null)
                    .mapToDouble(f -> f.getFoodItem().getProteinPer100g().doubleValue() * f.getAmountGrams() / 100.0)
                    .sum();
            double carbs = dayFood.stream()
                    .filter(f -> f.getFoodItem() != null)
                    .mapToDouble(f -> f.getFoodItem().getCarbsPer100g().doubleValue() * f.getAmountGrams() / 100.0)
                    .sum();
            double fat = dayFood.stream()
                    .filter(f -> f.getFoodItem() != null)
                    .mapToDouble(f -> f.getFoodItem().getFatPer100g().doubleValue() * f.getAmountGrams() / 100.0)
                    .sum();
            int waterMl = dayWater.stream().mapToInt(WaterLog::getAmountMl).sum();

            points.add(DashboardDtos.DailyDataPoint.builder()
                    .date(d.toString())
                    .calories(round(calories))
                    .protein(round(protein))
                    .carbs(round(carbs))
                    .fat(round(fat))
                    .waterMl(waterMl)
                    .weightKg(dayWeight != null ? dayWeight.getWeightKg() : null)
                    .build());
        }

        return points;
    }

    private DashboardDtos.WeightSnapshot buildWeightSnapshot(Long userId, Goal goal) {
        Optional<WeightLog> latest = weightLogRepository.findLatestByUserId(userId);
        if (latest.isEmpty()) return null;

        BigDecimal current = latest.get().getWeightKg();
        BigDecimal starting = goal != null ? goal.getStartingWeightKg() : null;
        BigDecimal target = goal != null ? goal.getTargetWeightKg() : null;

        Integer progress = null;
        if (starting != null && target != null) {
            double totalChange = starting.subtract(target).doubleValue();
            double achieved = starting.subtract(current).doubleValue();
            if (totalChange != 0) {
                progress = (int) Math.round((achieved / totalChange) * 100);
            }
        }

        return DashboardDtos.WeightSnapshot.builder()
                .current(current)
                .starting(starting)
                .target(target)
                .progress(progress)
                .build();
    }

    private int toPercent(double actual, int goal) {
        if (goal == 0) return 0;
        return (int) Math.min(Math.round((actual / goal) * 100), 100);
    }

    private double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
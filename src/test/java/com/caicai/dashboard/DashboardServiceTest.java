package com.caicai.dashboard;

import com.caicai.food.FoodItem;
import com.caicai.goal.Goal;
import com.caicai.goal.GoalRepository;
import com.caicai.log.FoodLog;
import com.caicai.log.FoodLog.MealType;
import com.caicai.log.FoodLogDtos.FoodLogResponse;
import com.caicai.log.FoodLogRepository;
import com.caicai.log.FoodLogService;
import com.caicai.water.WaterLog;
import com.caicai.water.WaterLogRepository;
import com.caicai.weight.WeightLog;
import com.caicai.weight.WeightLogRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock private FoodLogRepository foodLogRepository;
    @Mock private FoodLogService foodLogService;
    @Mock private WaterLogRepository waterLogRepository;
    @Mock private WeightLogRepository weightLogRepository;
    @Mock private GoalRepository goalRepository;

    @InjectMocks private DashboardService service;

    // -------------------------------------------------------------------------
    // getDailySummary — totals
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("getDailySummary: sums calories and macros from all food entries")
    void getDailySummary_sumsMacrosFromAllEntries() {
        LocalDate date = LocalDate.of(2024, 1, 15);

        List<FoodLogResponse> entries = List.of(
                buildFoodLogResponse(400.0, 30.0, 50.0, 10.0),
                buildFoodLogResponse(600.0, 40.0, 80.0, 20.0)
        );
        when(foodLogService.getByDate(1L, date)).thenReturn(entries);
        when(waterLogRepository.findByUserIdAndDate(1L, date)).thenReturn(List.of());
        when(goalRepository.findActiveGoalForDate(1L, date)).thenReturn(Optional.empty());
        when(weightLogRepository.findLatestByUserId(1L)).thenReturn(Optional.empty());

        DashboardDtos.DailySummary result = service.getDailySummary(1L, date);

        assertThat(result.getTotals().getCalories()).isEqualTo(1000.0);
        assertThat(result.getTotals().getProtein()).isEqualTo(70.0);
        assertThat(result.getTotals().getCarbs()).isEqualTo(130.0);
        assertThat(result.getTotals().getFat()).isEqualTo(30.0);
    }

    @Test
    @DisplayName("getDailySummary: sums water from all water log entries")
    void getDailySummary_sumsWaterEntries() {
        LocalDate date = LocalDate.of(2024, 1, 15);

        when(foodLogService.getByDate(1L, date)).thenReturn(List.of());
        when(waterLogRepository.findByUserIdAndDate(1L, date)).thenReturn(List.of(
                buildWaterLog(500),
                buildWaterLog(750)
        ));
        when(goalRepository.findActiveGoalForDate(1L, date)).thenReturn(Optional.empty());
        when(weightLogRepository.findLatestByUserId(1L)).thenReturn(Optional.empty());

        DashboardDtos.DailySummary result = service.getDailySummary(1L, date);

        assertThat(result.getTotals().getWaterMl()).isEqualTo(1250);
    }

    @Test
    @DisplayName("getDailySummary: returns zero totals when no entries exist")
    void getDailySummary_returnsZerosWhenEmpty() {
        LocalDate date = LocalDate.of(2024, 1, 15);

        when(foodLogService.getByDate(1L, date)).thenReturn(List.of());
        when(waterLogRepository.findByUserIdAndDate(1L, date)).thenReturn(List.of());
        when(goalRepository.findActiveGoalForDate(1L, date)).thenReturn(Optional.empty());
        when(weightLogRepository.findLatestByUserId(1L)).thenReturn(Optional.empty());

        DashboardDtos.DailySummary result = service.getDailySummary(1L, date);

        assertThat(result.getTotals().getCalories()).isEqualTo(0.0);
        assertThat(result.getTotals().getProtein()).isEqualTo(0.0);
        assertThat(result.getTotals().getWaterMl()).isEqualTo(0);
    }

    // -------------------------------------------------------------------------
    // getDailySummary — progress percentages
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("getDailySummary: calculates progress percentages correctly")
    void getDailySummary_calculatesProgressPercentages() {
        LocalDate date = LocalDate.of(2024, 1, 15);

        when(foodLogService.getByDate(1L, date)).thenReturn(List.of(
                buildFoodLogResponse(1050.0, 80.0, 105.0, 35.0)
        ));
        when(waterLogRepository.findByUserIdAndDate(1L, date)).thenReturn(List.of(
                buildWaterLog(1250)
        ));

        Goal goal = buildGoal(2100, 160, 210, 70, 2500);
        when(goalRepository.findActiveGoalForDate(1L, date)).thenReturn(Optional.of(goal));
        when(weightLogRepository.findLatestByUserId(1L)).thenReturn(Optional.empty());

        DashboardDtos.DailySummary result = service.getDailySummary(1L, date);

        assertThat(result.getProgress().getCalories()).isEqualTo(50);   // 1050/2100
        assertThat(result.getProgress().getProtein()).isEqualTo(50);    // 80/160
        assertThat(result.getProgress().getCarbs()).isEqualTo(50);      // 105/210
        assertThat(result.getProgress().getFat()).isEqualTo(50);        // 35/70
        assertThat(result.getProgress().getWaterMl()).isEqualTo(50);    // 1250/2500
    }

    @Test
    @DisplayName("getDailySummary: progress is capped at 100 when actual exceeds goal")
    void getDailySummary_progressCappedAt100() {
        LocalDate date = LocalDate.of(2024, 1, 15);

        // 3000 kcal consumed against 2000 kcal goal — should be 100, not 150
        when(foodLogService.getByDate(1L, date)).thenReturn(List.of(
                buildFoodLogResponse(3000.0, 200.0, 300.0, 100.0)
        ));
        when(waterLogRepository.findByUserIdAndDate(1L, date)).thenReturn(List.of());

        Goal goal = buildGoal(2000, 150, 200, 70, 2500);
        when(goalRepository.findActiveGoalForDate(1L, date)).thenReturn(Optional.of(goal));
        when(weightLogRepository.findLatestByUserId(1L)).thenReturn(Optional.empty());

        DashboardDtos.DailySummary result = service.getDailySummary(1L, date);

        assertThat(result.getProgress().getCalories()).isEqualTo(100);
        assertThat(result.getProgress().getProtein()).isEqualTo(100);
    }

    @Test
    @DisplayName("getDailySummary: progress is 0 when goal is 0 (no division by zero)")
    void getDailySummary_progressZeroWhenGoalIsZero() {
        LocalDate date = LocalDate.of(2024, 1, 15);

        when(foodLogService.getByDate(1L, date)).thenReturn(List.of(
                buildFoodLogResponse(500.0, 30.0, 60.0, 15.0)
        ));
        when(waterLogRepository.findByUserIdAndDate(1L, date)).thenReturn(List.of());

        // Goal with all zeros — guard against division by zero
        Goal goal = buildGoal(0, 0, 0, 0, 0);
        when(goalRepository.findActiveGoalForDate(1L, date)).thenReturn(Optional.of(goal));
        when(weightLogRepository.findLatestByUserId(1L)).thenReturn(Optional.empty());

        DashboardDtos.DailySummary result = service.getDailySummary(1L, date);

        assertThat(result.getProgress().getCalories()).isEqualTo(0);
        assertThat(result.getProgress().getProtein()).isEqualTo(0);
        assertThat(result.getProgress().getWaterMl()).isEqualTo(0);
    }

    @Test
    @DisplayName("getDailySummary: progress is null when no goal exists")
    void getDailySummary_progressNullWhenNoGoal() {
        LocalDate date = LocalDate.of(2024, 1, 15);

        when(foodLogService.getByDate(1L, date)).thenReturn(List.of());
        when(waterLogRepository.findByUserIdAndDate(1L, date)).thenReturn(List.of());
        when(goalRepository.findActiveGoalForDate(1L, date)).thenReturn(Optional.empty());
        when(weightLogRepository.findLatestByUserId(1L)).thenReturn(Optional.empty());

        DashboardDtos.DailySummary result = service.getDailySummary(1L, date);

        assertThat(result.getProgress()).isNull();
        assertThat(result.getGoal()).isNull();
    }

    // -------------------------------------------------------------------------
    // getDailySummary — weight snapshot
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("getDailySummary: weight snapshot is null when no weight logs exist")
    void getDailySummary_weightSnapshotNullWhenNoLogs() {
        LocalDate date = LocalDate.of(2024, 1, 15);

        when(foodLogService.getByDate(1L, date)).thenReturn(List.of());
        when(waterLogRepository.findByUserIdAndDate(1L, date)).thenReturn(List.of());
        when(goalRepository.findActiveGoalForDate(1L, date)).thenReturn(Optional.empty());
        when(weightLogRepository.findLatestByUserId(1L)).thenReturn(Optional.empty());

        DashboardDtos.DailySummary result = service.getDailySummary(1L, date);

        assertThat(result.getWeight()).isNull();
    }

    @Test
    @DisplayName("getDailySummary: weight snapshot includes current weight when log exists")
    void getDailySummary_weightSnapshotIncludesCurrentWeight() {
        LocalDate date = LocalDate.of(2024, 1, 15);

        when(foodLogService.getByDate(1L, date)).thenReturn(List.of());
        when(waterLogRepository.findByUserIdAndDate(1L, date)).thenReturn(List.of());
        when(goalRepository.findActiveGoalForDate(1L, date)).thenReturn(Optional.empty());
        when(weightLogRepository.findLatestByUserId(1L)).thenReturn(Optional.of(
                buildWeightLog(new BigDecimal("82.5"))
        ));

        DashboardDtos.DailySummary result = service.getDailySummary(1L, date);

        assertThat(result.getWeight()).isNotNull();
        assertThat(result.getWeight().getCurrent()).isEqualByComparingTo(new BigDecimal("82.5"));
    }

    @Test
    @DisplayName("getDailySummary: weight progress is calculated correctly")
    void getDailySummary_weightProgressCalculated() {
        LocalDate date = LocalDate.of(2024, 1, 15);

        when(foodLogService.getByDate(1L, date)).thenReturn(List.of());
        when(waterLogRepository.findByUserIdAndDate(1L, date)).thenReturn(List.of());

        // Starting 90kg, target 80kg, current 85kg — halfway there = 50%
        Goal goal = buildGoal(2000, 150, 200, 70, 2500);
        goal.setStartingWeightKg(new BigDecimal("90.0"));
        goal.setTargetWeightKg(new BigDecimal("80.0"));
        when(goalRepository.findActiveGoalForDate(1L, date)).thenReturn(Optional.of(goal));
        when(weightLogRepository.findLatestByUserId(1L)).thenReturn(Optional.of(
                buildWeightLog(new BigDecimal("85.0"))
        ));

        DashboardDtos.DailySummary result = service.getDailySummary(1L, date);

        assertThat(result.getWeight().getProgress()).isEqualTo(50);
    }

    // -------------------------------------------------------------------------
    // getDailySummary — date field
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("getDailySummary: date field matches the requested date")
    void getDailySummary_dateFieldMatchesRequest() {
        LocalDate date = LocalDate.of(2024, 3, 22);

        when(foodLogService.getByDate(1L, date)).thenReturn(List.of());
        when(waterLogRepository.findByUserIdAndDate(1L, date)).thenReturn(List.of());
        when(goalRepository.findActiveGoalForDate(1L, date)).thenReturn(Optional.empty());
        when(weightLogRepository.findLatestByUserId(1L)).thenReturn(Optional.empty());

        DashboardDtos.DailySummary result = service.getDailySummary(1L, date);

        assertThat(result.getDate()).isEqualTo("2024-03-22");
    }

    // -------------------------------------------------------------------------
    // getWeeklySummary — data points
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("getWeeklySummary: returns exactly 7 data points")
    void getWeeklySummary_returns7DataPoints() {
        LocalDate date = LocalDate.of(2024, 1, 21);  // Sunday
        LocalDate from = date.minusDays(6);           // Monday

        when(foodLogRepository.findByUserIdAndDateBetween(1L, from, date)).thenReturn(List.of());
        when(waterLogRepository.findByUserIdAndDateBetween(1L, from, date)).thenReturn(List.of());
        when(weightLogRepository.findByUserIdAndDateBetween(1L, from, date)).thenReturn(List.of());

        List<DashboardDtos.DailyDataPoint> result = service.getWeeklySummary(1L, date);

        assertThat(result).hasSize(7);
    }

    @Test
    @DisplayName("getWeeklySummary: data points are in ascending date order")
    void getWeeklySummary_pointsInAscendingDateOrder() {
        LocalDate date = LocalDate.of(2024, 1, 21);
        LocalDate from = date.minusDays(6);

        when(foodLogRepository.findByUserIdAndDateBetween(1L, from, date)).thenReturn(List.of());
        when(waterLogRepository.findByUserIdAndDateBetween(1L, from, date)).thenReturn(List.of());
        when(weightLogRepository.findByUserIdAndDateBetween(1L, from, date)).thenReturn(List.of());

        List<DashboardDtos.DailyDataPoint> result = service.getWeeklySummary(1L, date);

        assertThat(result.get(0).getDate()).isEqualTo(from.toString());
        assertThat(result.get(6).getDate()).isEqualTo(date.toString());
    }

    @Test
    @DisplayName("getWeeklySummary: food macros are aggregated per day correctly")
    void getWeeklySummary_aggregatesFoodMacrosPerDay() {
        LocalDate date = LocalDate.of(2024, 1, 21);
        LocalDate from = date.minusDays(6);
        LocalDate targetDay = LocalDate.of(2024, 1, 18);  // Thursday in the range

        FoodItem food = buildFoodItem(new BigDecimal("200"), new BigDecimal("20"),
                new BigDecimal("40"), new BigDecimal("10"));

        // Two logs on Thursday: 100g + 150g
        List<FoodLog> foodLogs = List.of(
                buildFoodLog(food, 100, targetDay),
                buildFoodLog(food, 150, targetDay)
        );
        when(foodLogRepository.findByUserIdAndDateBetween(1L, from, date)).thenReturn(foodLogs);
        when(waterLogRepository.findByUserIdAndDateBetween(1L, from, date)).thenReturn(List.of());
        when(weightLogRepository.findByUserIdAndDateBetween(1L, from, date)).thenReturn(List.of());

        List<DashboardDtos.DailyDataPoint> result = service.getWeeklySummary(1L, date);

        DashboardDtos.DailyDataPoint thursday = result.stream()
                .filter(p -> p.getDate().equals(targetDay.toString()))
                .findFirst()
                .orElseThrow();

        // 100g + 150g = 250g at 200 kcal/100g = 500 kcal
        assertThat(thursday.getCalories()).isEqualTo(500.0);
        assertThat(thursday.getProtein()).isEqualTo(50.0);
    }

    @Test
    @DisplayName("getWeeklySummary: days with no data have zero calories and null weightKg")
    void getWeeklySummary_emptyDaysHaveZeroCaloriesAndNullWeight() {
        LocalDate date = LocalDate.of(2024, 1, 21);
        LocalDate from = date.minusDays(6);

        when(foodLogRepository.findByUserIdAndDateBetween(1L, from, date)).thenReturn(List.of());
        when(waterLogRepository.findByUserIdAndDateBetween(1L, from, date)).thenReturn(List.of());
        when(weightLogRepository.findByUserIdAndDateBetween(1L, from, date)).thenReturn(List.of());

        List<DashboardDtos.DailyDataPoint> result = service.getWeeklySummary(1L, date);

        result.forEach(p -> {
            assertThat(p.getCalories()).isEqualTo(0.0);
            assertThat(p.getWeightKg()).isNull();
        });
    }

    @Test
    @DisplayName("getMonthlySummary: returns exactly 30 data points")
    void getMonthlySummary_returns30DataPoints() {
        LocalDate date = LocalDate.of(2024, 1, 31);
        LocalDate from = date.minusDays(29);

        when(foodLogRepository.findByUserIdAndDateBetween(1L, from, date)).thenReturn(List.of());
        when(waterLogRepository.findByUserIdAndDateBetween(1L, from, date)).thenReturn(List.of());
        when(weightLogRepository.findByUserIdAndDateBetween(1L, from, date)).thenReturn(List.of());

        List<DashboardDtos.DailyDataPoint> result = service.getMonthlySummary(1L, date);

        assertThat(result).hasSize(30);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private FoodLogResponse buildFoodLogResponse(double calories, double protein,
                                                 double carbs, double fat) {
        return FoodLogResponse.builder()
                .id(1L)
                .foodItemId(1L)
                .foodName("Test Food")
                .amountGrams(100)
                .mealType(MealType.LUNCH)
                .calories(calories)
                .protein(protein)
                .carbs(carbs)
                .fat(fat)
                .build();
    }

    private WaterLog buildWaterLog(int amountMl) {
        WaterLog log = new WaterLog();
        log.setAmountMl(amountMl);
        log.setLoggedAt(LocalDateTime.now());
        return log;
    }

    private WeightLog buildWeightLog(BigDecimal weightKg) {
        WeightLog log = new WeightLog();
        log.setWeightKg(weightKg);
        log.setLoggedAt(LocalDateTime.now());
        return log;
    }

    private Goal buildGoal(int calories, int protein, int carbs, int fat, int waterMl) {
        Goal goal = new Goal();
        goal.setCalories(calories);
        goal.setProtein(protein);
        goal.setCarbs(carbs);
        goal.setFat(fat);
        goal.setWaterMl(waterMl);
        return goal;
    }

    private FoodItem buildFoodItem(BigDecimal calories, BigDecimal protein,
                                   BigDecimal carbs, BigDecimal fat) {
        FoodItem food = new FoodItem();
        food.setId(1L);
        food.setName("Test Food");
        food.setCaloriesPer100g(calories);
        food.setProteinPer100g(protein);
        food.setCarbsPer100g(carbs);
        food.setFatPer100g(fat);
        return food;
    }

    private FoodLog buildFoodLog(FoodItem food, int amountGrams, LocalDate date) {
        FoodLog log = new FoodLog();
        log.setFoodItem(food);
        log.setAmountGrams(amountGrams);
        log.setMealType(MealType.LUNCH);
        log.setLoggedAt(date.atStartOfDay());
        return log;
    }
}
package com.caicai.log;

import com.caicai.common.AppException;
import com.caicai.food.FoodItem;
import com.caicai.food.FoodItemRepository;
import com.caicai.log.FoodLog.MealType;
import com.caicai.log.FoodLogDtos.CreateFoodLogRequest;
import com.caicai.log.FoodLogDtos.FoodLogResponse;
import com.caicai.log.FoodLogDtos.UpdateFoodLogRequest;
import com.caicai.user.User;
import com.caicai.user.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FoodLogServiceTest {

    @Mock private FoodLogRepository foodLogRepository;
    @Mock private FoodItemRepository foodItemRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private FoodLogService service;

    // -------------------------------------------------------------------------
    // Macro calculation
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("create: macros are scaled correctly to amountGrams")
    void create_macrosScaledToAmountGrams() {
        // Arrange — food item with known per-100g values, logged at 200g
        FoodItem food = buildFoodItem(1L, new BigDecimal("200"), new BigDecimal("10"),
                new BigDecimal("30"), new BigDecimal("5"));
        User user = buildUser(1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(foodItemRepository.findById(1L)).thenReturn(Optional.of(food));
        when(foodLogRepository.save(any())).thenAnswer(inv -> {
            FoodLog log = inv.getArgument(0);
            log.setId(99L);
            return log;
        });

        CreateFoodLogRequest dto = new CreateFoodLogRequest(1L, 200, MealType.LUNCH, LocalDate.now());

        // Act
        FoodLogResponse result = service.create(1L, dto);

        // Assert — 200g at 200 kcal/100g = 400 kcal, etc.
        assertThat(result.getCalories()).isEqualTo(400.0);
        assertThat(result.getProtein()).isEqualTo(20.0);
        assertThat(result.getCarbs()).isEqualTo(60.0);
        assertThat(result.getFat()).isEqualTo(10.0);
    }

    @Test
    @DisplayName("create: macros are rounded to 1 decimal place")
    void create_macrosRoundedToOneDecimal() {
        // Arrange — 333g at 100 kcal/100g = 333.0 (exact); use 1g at 10.15 kcal/100g = 0.1015 → 0.1
        FoodItem food = buildFoodItem(1L, new BigDecimal("10.15"), new BigDecimal("1.15"),
                new BigDecimal("2.15"), new BigDecimal("0.15"));
        User user = buildUser(1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(foodItemRepository.findById(1L)).thenReturn(Optional.of(food));
        when(foodLogRepository.save(any())).thenAnswer(inv -> {
            FoodLog log = inv.getArgument(0);
            log.setId(99L);
            return log;
        });

        CreateFoodLogRequest dto = new CreateFoodLogRequest(1L, 1, MealType.BREAKFAST, LocalDate.now());

        // Act
        FoodLogResponse result = service.create(1L, dto);

        // Assert — 1g / 100 * 10.15 = 0.1015 → rounded to 0.1
        assertThat(result.getCalories()).isEqualTo(0.1);
        assertThat(result.getProtein()).isEqualTo(0.0);
        assertThat(result.getCarbs()).isEqualTo(0.0);
        assertThat(result.getFat()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("create: throws NOT_FOUND when user does not exist")
    void create_throwsWhenUserNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        CreateFoodLogRequest dto = new CreateFoodLogRequest(1L, 100, MealType.DINNER, LocalDate.now());

        assertThatThrownBy(() -> service.create(99L, dto))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    @DisplayName("create: throws NOT_FOUND when food item does not exist")
    void create_throwsWhenFoodItemNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(buildUser(1L)));
        when(foodItemRepository.findById(99L)).thenReturn(Optional.empty());

        CreateFoodLogRequest dto = new CreateFoodLogRequest(99L, 100, MealType.DINNER, LocalDate.now());

        assertThatThrownBy(() -> service.create(1L, dto))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    // -------------------------------------------------------------------------
    // getByDate
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("getByDate: returns empty list when no entries for date")
    void getByDate_returnsEmptyListWhenNoEntries() {
        when(foodLogRepository.findByUserIdAndDate(1L, LocalDate.now())).thenReturn(List.of());

        List<FoodLogResponse> result = service.getByDate(1L, LocalDate.now());

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getByDate: maps all entries for the date")
    void getByDate_mapsAllEntries() {
        FoodItem food = buildFoodItem(1L, new BigDecimal("100"), new BigDecimal("10"),
                new BigDecimal("20"), new BigDecimal("5"));
        User user = buildUser(1L);

        List<FoodLog> logs = List.of(
                buildLog(1L, user, food, 100, MealType.BREAKFAST),
                buildLog(2L, user, food, 200, MealType.LUNCH)
        );
        when(foodLogRepository.findByUserIdAndDate(1L, LocalDate.now())).thenReturn(logs);

        List<FoodLogResponse> result = service.getByDate(1L, LocalDate.now());

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getAmountGrams()).isEqualTo(100);
        assertThat(result.get(1).getAmountGrams()).isEqualTo(200);
    }

    // -------------------------------------------------------------------------
    // delete — ownership
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("delete: succeeds when user owns the log entry")
    void delete_succeedsForOwner() {
        User user = buildUser(1L);
        FoodLog log = buildLog(10L, user, buildFoodItem(1L), 100, MealType.SNACK);

        when(foodLogRepository.findById(10L)).thenReturn(Optional.of(log));

        assertThatCode(() -> service.delete(1L, 10L)).doesNotThrowAnyException();
        verify(foodLogRepository).delete(log);
    }

    @Test
    @DisplayName("delete: throws FORBIDDEN when user does not own the log entry")
    void delete_throwsForbiddenForNonOwner() {
        User owner = buildUser(1L);
        FoodLog log = buildLog(10L, owner, buildFoodItem(1L), 100, MealType.SNACK);

        when(foodLogRepository.findById(10L)).thenReturn(Optional.of(log));

        assertThatThrownBy(() -> service.delete(2L, 10L))  // different userId
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));

        verify(foodLogRepository, never()).delete(any());
    }

    @Test
    @DisplayName("delete: throws NOT_FOUND when log entry does not exist")
    void delete_throwsNotFoundWhenLogMissing() {
        when(foodLogRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(1L, 99L))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    // -------------------------------------------------------------------------
    // update — ownership + recalculation
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("update: recalculates macros after amount change")
    void update_recalculatesMacrosAfterAmountChange() {
        User user = buildUser(1L);
        FoodItem food = buildFoodItem(1L, new BigDecimal("200"), new BigDecimal("20"),
                new BigDecimal("40"), new BigDecimal("10"));
        FoodLog log = buildLog(10L, user, food, 100, MealType.LUNCH);  // originally 100g

        when(foodLogRepository.findById(10L)).thenReturn(Optional.of(log));

        UpdateFoodLogRequest dto = new UpdateFoodLogRequest(300);  // change to 300g
        FoodLogResponse result = service.update(1L, 10L, dto);

        // 300g at 200 kcal/100g = 600 kcal
        assertThat(result.getCalories()).isEqualTo(600.0);
        assertThat(result.getProtein()).isEqualTo(60.0);
        assertThat(result.getCarbs()).isEqualTo(120.0);
        assertThat(result.getFat()).isEqualTo(30.0);
        assertThat(result.getAmountGrams()).isEqualTo(300);
    }

    @Test
    @DisplayName("update: throws FORBIDDEN when user does not own the log entry")
    void update_throwsForbiddenForNonOwner() {
        User owner = buildUser(1L);
        FoodLog log = buildLog(10L, owner, buildFoodItem(1L), 100, MealType.LUNCH);

        when(foodLogRepository.findById(10L)).thenReturn(Optional.of(log));

        assertThatThrownBy(() -> service.update(2L, 10L, new UpdateFoodLogRequest(200)))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    @DisplayName("update: throws NOT_FOUND when log entry does not exist")
    void update_throwsNotFoundWhenLogMissing() {
        when(foodLogRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(1L, 99L, new UpdateFoodLogRequest(100)))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    // -------------------------------------------------------------------------
    // copyDay
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("copyDay: copies all entries to target date")
    void copyDay_copiesAllEntriesToTargetDate() {
        User user = buildUser(1L);
        FoodItem food = buildFoodItem(1L);
        LocalDate source = LocalDate.of(2024, 1, 14);
        LocalDate target = LocalDate.of(2024, 1, 15);

        List<FoodLog> sourceLogs = List.of(
                buildLog(1L, user, food, 100, MealType.BREAKFAST),
                buildLog(2L, user, food, 150, MealType.LUNCH)
        );
        when(foodLogRepository.findByUserIdAndDate(1L, source)).thenReturn(sourceLogs);

        service.copyDay(1L, source, target);

        ArgumentCaptor<List<FoodLog>> captor = ArgumentCaptor.forClass(List.class);
        verify(foodLogRepository).saveAll(captor.capture());

        List<FoodLog> saved = captor.getValue();
        assertThat(saved).hasSize(2);
        assertThat(saved.get(0).getAmountGrams()).isEqualTo(100);
        assertThat(saved.get(1).getAmountGrams()).isEqualTo(150);
        // All copied entries should be dated to target
        saved.forEach(e -> assertThat(e.getLoggedAt().toLocalDate()).isEqualTo(target));
    }

    @Test
    @DisplayName("copyDay: throws BAD_REQUEST when source date has no entries")
    void copyDay_throwsBadRequestWhenSourceEmpty() {
        LocalDate source = LocalDate.of(2024, 1, 14);
        LocalDate target = LocalDate.of(2024, 1, 15);

        when(foodLogRepository.findByUserIdAndDate(1L, source)).thenReturn(List.of());

        assertThatThrownBy(() -> service.copyDay(1L, source, target))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));

        verify(foodLogRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("copyDay: preserves food item and meal type on copied entries")
    void copyDay_preservesFoodItemAndMealType() {
        User user = buildUser(1L);
        FoodItem food = buildFoodItem(5L);
        LocalDate source = LocalDate.of(2024, 1, 14);
        LocalDate target = LocalDate.of(2024, 1, 15);

        when(foodLogRepository.findByUserIdAndDate(1L, source))
                .thenReturn(List.of(buildLog(1L, user, food, 200, MealType.DINNER)));

        service.copyDay(1L, source, target);

        ArgumentCaptor<List<FoodLog>> captor = ArgumentCaptor.forClass(List.class);
        verify(foodLogRepository).saveAll(captor.capture());

        FoodLog copy = captor.getValue().get(0);
        assertThat(copy.getFoodItem().getId()).isEqualTo(5L);
        assertThat(copy.getMealType()).isEqualTo(MealType.DINNER);
        assertThat(copy.getUser().getId()).isEqualTo(1L);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private User buildUser(Long id) {
        User user = new User();
        user.setId(id);
        return user;
    }

    private FoodItem buildFoodItem(Long id) {
        return buildFoodItem(id, new BigDecimal("100"), new BigDecimal("10"),
                new BigDecimal("20"), new BigDecimal("5"));
    }

    private FoodItem buildFoodItem(Long id, BigDecimal calories, BigDecimal protein,
                                   BigDecimal carbs, BigDecimal fat) {
        FoodItem food = new FoodItem();
        food.setId(id);
        food.setName("Test Food");
        food.setBrand("Test Brand");
        food.setCaloriesPer100g(calories);
        food.setProteinPer100g(protein);
        food.setCarbsPer100g(carbs);
        food.setFatPer100g(fat);
        return food;
    }

    private FoodLog buildLog(Long id, User user, FoodItem food, int amountGrams, MealType mealType) {
        FoodLog log = new FoodLog();
        log.setId(id);
        log.setUser(user);
        log.setFoodItem(food);
        log.setAmountGrams(amountGrams);
        log.setMealType(mealType);
        log.setLoggedAt(LocalDateTime.now());
        return log;
    }
}
package com.caicai.goal;

import com.caicai.common.AppException;
import com.caicai.user.User;
import com.caicai.user.UserRepository;
import com.caicai.weight.WeightLog;
import com.caicai.weight.WeightLogRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GoalServiceTest {

    @Mock private GoalRepository goalRepository;
    @Mock private WeightLogRepository weightLogRepository;
    @Mock private UserRepository userRepository;
    @Mock private RestTemplate restTemplate;
    @Mock private ObjectMapper objectMapper;

    @InjectMocks private GoalService service;

    // -------------------------------------------------------------------------
    // getCurrentGoal
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("getCurrentGoal: returns goal response when active goal exists")
    void getCurrentGoal_returnsGoalWhenExists() {
        User user = buildUser(1L);
        Goal goal = buildGoal(1L, user, 2000, 150, 200, 70, 2500);

        when(goalRepository.findActiveGoalForDate(eq(1L), any(LocalDate.class)))
                .thenReturn(Optional.of(goal));

        GoalDtos.GoalResponse result = service.getCurrentGoal(user);

        assertThat(result.calories()).isEqualTo(2000);
        assertThat(result.protein()).isEqualTo(150);
        assertThat(result.carbs()).isEqualTo(200);
        assertThat(result.fat()).isEqualTo(70);
        assertThat(result.waterMl()).isEqualTo(2500);
    }

    @Test
    @DisplayName("getCurrentGoal: throws NOT_FOUND when no active goal exists")
    void getCurrentGoal_throwsNotFoundWhenNoGoal() {
        User user = buildUser(1L);

        when(goalRepository.findActiveGoalForDate(eq(1L), any(LocalDate.class)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getCurrentGoal(user))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    // -------------------------------------------------------------------------
    // createGoal
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("createGoal: saves a new goal with effectiveFrom set to today")
    void createGoal_savesGoalWithEffectiveFromToday() {
        User user = buildUser(1L);
        GoalDtos.CreateGoalRequest request = buildCreateRequest(2000, 150, 200, 70, 2500,
                new BigDecimal("80.0"), new BigDecimal("75.0"));

        when(goalRepository.save(any(Goal.class))).thenAnswer(inv -> {
            Goal g = inv.getArgument(0);
            g.setId(10L);
            return g;
        });
        when(weightLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        GoalDtos.GoalResponse result = service.createGoal(user, request);

        ArgumentCaptor<Goal> goalCaptor = ArgumentCaptor.forClass(Goal.class);
        verify(goalRepository).save(goalCaptor.capture());

        Goal saved = goalCaptor.getValue();
        assertThat(saved.getEffectiveFrom()).isEqualTo(LocalDate.now());
        assertThat(saved.getCalories()).isEqualTo(2000);
        assertThat(saved.getProtein()).isEqualTo(150);
        assertThat(saved.getCarbs()).isEqualTo(200);
        assertThat(saved.getFat()).isEqualTo(70);
        assertThat(saved.getWaterMl()).isEqualTo(2500);
    }

    @Test
    @DisplayName("createGoal: also saves a weight log entry for the starting weight")
    void createGoal_savesWeightLogForStartingWeight() {
        User user = buildUser(1L);
        GoalDtos.CreateGoalRequest request = buildCreateRequest(2000, 150, 200, 70, 2500,
                new BigDecimal("82.5"), new BigDecimal("75.0"));

        when(goalRepository.save(any())).thenAnswer(inv -> {
            Goal g = inv.getArgument(0);
            g.setId(10L);
            return g;
        });
        when(weightLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.createGoal(user, request);

        ArgumentCaptor<WeightLog> weightCaptor = ArgumentCaptor.forClass(WeightLog.class);
        verify(weightLogRepository).save(weightCaptor.capture());

        WeightLog savedWeight = weightCaptor.getValue();
        assertThat(savedWeight.getWeightKg()).isEqualByComparingTo(new BigDecimal("82.5"));
        assertThat(savedWeight.getUser()).isEqualTo(user);
    }

    @Test
    @DisplayName("createGoal: marks user onboarding as completed")
    void createGoal_marksOnboardingComplete() {
        User user = buildUser(1L);
        user.setHasCompletedOnboarding(false);

        GoalDtos.CreateGoalRequest request = buildCreateRequest(2000, 150, 200, 70, 2500,
                new BigDecimal("80.0"), new BigDecimal("75.0"));

        when(goalRepository.save(any())).thenAnswer(inv -> {
            Goal g = inv.getArgument(0);
            g.setId(10L);
            return g;
        });
        when(weightLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.createGoal(user, request);

        assertThat(user.isHasCompletedOnboarding()).isTrue();
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("createGoal: stores correct starting and target weight on the goal")
    void createGoal_storesWeightsOnGoal() {
        User user = buildUser(1L);
        BigDecimal starting = new BigDecimal("90.0");
        BigDecimal target   = new BigDecimal("80.0");

        GoalDtos.CreateGoalRequest request = buildCreateRequest(2000, 150, 200, 70, 2500,
                starting, target);

        when(goalRepository.save(any())).thenAnswer(inv -> {
            Goal g = inv.getArgument(0);
            g.setId(10L);
            return g;
        });
        when(weightLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.createGoal(user, request);

        ArgumentCaptor<Goal> goalCaptor = ArgumentCaptor.forClass(Goal.class);
        verify(goalRepository).save(goalCaptor.capture());

        Goal saved = goalCaptor.getValue();
        assertThat(saved.getStartingWeightKg()).isEqualByComparingTo(starting);
        assertThat(saved.getTargetWeightKg()).isEqualByComparingTo(target);
    }

    // -------------------------------------------------------------------------
    // getGoalHistory — 1-indexed pagination
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("getGoalHistory: converts 1-indexed page to 0-indexed for repository")
    void getGoalHistory_convertsPageIndexCorrectly() {
        User user = buildUser(1L);
        Page<Goal> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);

        when(goalRepository.findByUserIdOrderByEffectiveFromDesc(eq(1L), any()))
                .thenReturn(emptyPage);

        service.getGoalHistory(user, 1, 20);

        ArgumentCaptor<org.springframework.data.domain.Pageable> pageCaptor =
                ArgumentCaptor.forClass(org.springframework.data.domain.Pageable.class);
        verify(goalRepository).findByUserIdOrderByEffectiveFromDesc(eq(1L), pageCaptor.capture());

        // Page 1 from the frontend → page 0 in JPA
        assertThat(pageCaptor.getValue().getPageNumber()).isEqualTo(0);
        assertThat(pageCaptor.getValue().getPageSize()).isEqualTo(20);
    }

    @Test
    @DisplayName("getGoalHistory: maps goal fields to response correctly")
    void getGoalHistory_mapsGoalFieldsToResponse() {
        User user = buildUser(1L);
        Goal goal = buildGoal(5L, user, 1800, 140, 190, 65, 2000);
        goal.setEffectiveFrom(LocalDate.of(2024, 1, 1));

        Page<Goal> page = new PageImpl<>(List.of(goal), PageRequest.of(0, 20), 1);
        when(goalRepository.findByUserIdOrderByEffectiveFromDesc(eq(1L), any()))
                .thenReturn(page);

        Page<GoalDtos.GoalResponse> result = service.getGoalHistory(user, 1, 20);

        assertThat(result.getContent()).hasSize(1);
        GoalDtos.GoalResponse response = result.getContent().get(0);
        assertThat(response.id()).isEqualTo(5L);
        assertThat(response.calories()).isEqualTo(1800);
        assertThat(response.effectiveFrom()).isEqualTo("2024-01-01");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private User buildUser(Long id) {
        User user = new User();
        user.setId(id);
        return user;
    }

    private Goal buildGoal(Long id, User user, int calories, int protein,
                           int carbs, int fat, int waterMl) {
        Goal goal = new Goal();
        goal.setId(id);
        goal.setUser(user);
        goal.setCalories(calories);
        goal.setProtein(protein);
        goal.setCarbs(carbs);
        goal.setFat(fat);
        goal.setWaterMl(waterMl);
        goal.setStartingWeightKg(new BigDecimal("80.0"));
        goal.setTargetWeightKg(new BigDecimal("75.0"));
        goal.setEffectiveFrom(LocalDate.now());
        return goal;
    }

    private GoalDtos.CreateGoalRequest buildCreateRequest(int calories, int protein, int carbs,
                                                          int fat, int waterMl,
                                                          BigDecimal startingWeightKg,
                                                          BigDecimal targetWeightKg) {
        return new GoalDtos.CreateGoalRequest(
                calories, protein, carbs, fat, waterMl, startingWeightKg, targetWeightKg
        );
    }
}
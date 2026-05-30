package com.caicai.weight;

import com.caicai.common.AppException;
import com.caicai.user.User;
import com.caicai.user.UserRepository;
import com.caicai.weight.WeightDtos.CreateWeightRequest;
import com.caicai.weight.WeightDtos.WeightLogResponse;
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
class WeightServiceTest {

    @Mock private WeightLogRepository weightLogRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private WeightService service;

    // -------------------------------------------------------------------------
    // create — loggedAt timestamp logic
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("create: sets loggedAt to now when date is today")
    void create_setsLoggedAtToNowForToday() {
        User user = buildUser(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(weightLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        LocalDate today = LocalDate.now();
        CreateWeightRequest dto = new CreateWeightRequest(new BigDecimal("82.5"), today);

        service.create(1L, dto);

        ArgumentCaptor<WeightLog> captor = ArgumentCaptor.forClass(WeightLog.class);
        verify(weightLogRepository).save(captor.capture());

        WeightLog saved = captor.getValue();
        // loggedAt should be very close to now — within a few seconds
        assertThat(saved.getLoggedAt()).isAfter(LocalDateTime.now().minusSeconds(5));
        assertThat(saved.getLoggedAt()).isBefore(LocalDateTime.now().plusSeconds(5));
    }

    @Test
    @DisplayName("create: sets loggedAt to start of day when date is in the past")
    void create_setsLoggedAtToStartOfDayForPastDate() {
        User user = buildUser(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(weightLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        LocalDate pastDate = LocalDate.of(2024, 1, 10);
        CreateWeightRequest dto = new CreateWeightRequest(new BigDecimal("83.0"), pastDate);

        service.create(1L, dto);

        ArgumentCaptor<WeightLog> captor = ArgumentCaptor.forClass(WeightLog.class);
        verify(weightLogRepository).save(captor.capture());

        WeightLog saved = captor.getValue();
        assertThat(saved.getLoggedAt()).isEqualTo(pastDate.atStartOfDay());
    }

    @Test
    @DisplayName("create: stores the correct weightKg on the log entry")
    void create_storesCorrectWeight() {
        User user = buildUser(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(weightLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CreateWeightRequest dto = new CreateWeightRequest(new BigDecimal("78.3"), LocalDate.now());

        service.create(1L, dto);

        ArgumentCaptor<WeightLog> captor = ArgumentCaptor.forClass(WeightLog.class);
        verify(weightLogRepository).save(captor.capture());

        assertThat(captor.getValue().getWeightKg()).isEqualByComparingTo(new BigDecimal("78.3"));
    }

    @Test
    @DisplayName("create: throws NOT_FOUND when user does not exist")
    void create_throwsNotFoundWhenUserMissing() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        CreateWeightRequest dto = new CreateWeightRequest(new BigDecimal("80.0"), LocalDate.now());

        assertThatThrownBy(() -> service.create(99L, dto))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getStatus()).isEqualTo(HttpStatus.NOT_FOUND));

        verify(weightLogRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // delete — ownership
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("delete: succeeds when user owns the log entry")
    void delete_succeedsForOwner() {
        User user = buildUser(1L);
        WeightLog log = buildWeightLog(10L, user, new BigDecimal("82.0"));

        when(weightLogRepository.findById(10L)).thenReturn(Optional.of(log));

        assertThatCode(() -> service.delete(1L, 10L)).doesNotThrowAnyException();
        verify(weightLogRepository).delete(log);
    }

    @Test
    @DisplayName("delete: throws FORBIDDEN when user does not own the log entry")
    void delete_throwsForbiddenForNonOwner() {
        User owner = buildUser(1L);
        WeightLog log = buildWeightLog(10L, owner, new BigDecimal("82.0"));

        when(weightLogRepository.findById(10L)).thenReturn(Optional.of(log));

        assertThatThrownBy(() -> service.delete(2L, 10L))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));

        verify(weightLogRepository, never()).delete(any());
    }

    @Test
    @DisplayName("delete: throws NOT_FOUND when log entry does not exist")
    void delete_throwsNotFoundWhenLogMissing() {
        when(weightLogRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(1L, 99L))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    // -------------------------------------------------------------------------
    // getAll
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("getAll: returns empty list when no entries exist")
    void getAll_returnsEmptyListWhenNoEntries() {
        when(weightLogRepository.findByUserIdOrderByLoggedAtDesc(1L)).thenReturn(List.of());

        List<WeightLogResponse> result = service.getAll(1L);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getAll: maps weightKg and loggedAt correctly")
    void getAll_mapsFieldsCorrectly() {
        User user = buildUser(1L);
        LocalDateTime loggedAt = LocalDateTime.of(2024, 1, 15, 9, 30);
        WeightLog log = buildWeightLog(1L, user, new BigDecimal("81.5"));
        log.setLoggedAt(loggedAt);

        when(weightLogRepository.findByUserIdOrderByLoggedAtDesc(1L)).thenReturn(List.of(log));

        List<WeightLogResponse> result = service.getAll(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getWeightKg()).isEqualByComparingTo(new BigDecimal("81.5"));
        assertThat(result.get(0).getLoggedAt()).isEqualTo(loggedAt);
    }

    // -------------------------------------------------------------------------
    // getByDateRange
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("getByDateRange: returns only entries within the date range")
    void getByDateRange_returnsEntriesInRange() {
        User user = buildUser(1L);
        LocalDate from = LocalDate.of(2024, 1, 1);
        LocalDate to   = LocalDate.of(2024, 1, 31);

        List<WeightLog> logs = List.of(
                buildWeightLog(1L, user, new BigDecimal("82.0")),
                buildWeightLog(2L, user, new BigDecimal("81.5"))
        );
        when(weightLogRepository.findByUserIdAndDateBetween(1L, from, to)).thenReturn(logs);

        List<WeightLogResponse> result = service.getByDateRange(1L, from, to);

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("getByDateRange: returns empty list when no entries in range")
    void getByDateRange_returnsEmptyWhenNoEntries() {
        LocalDate from = LocalDate.of(2024, 1, 1);
        LocalDate to   = LocalDate.of(2024, 1, 31);

        when(weightLogRepository.findByUserIdAndDateBetween(1L, from, to)).thenReturn(List.of());

        List<WeightLogResponse> result = service.getByDateRange(1L, from, to);

        assertThat(result).isEmpty();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private User buildUser(Long id) {
        User user = new User();
        user.setId(id);
        return user;
    }

    private WeightLog buildWeightLog(Long id, User user, BigDecimal weightKg) {
        WeightLog log = new WeightLog();
        log.setId(id);
        log.setUser(user);
        log.setWeightKg(weightKg);
        log.setLoggedAt(LocalDateTime.now());
        return log;
    }
}
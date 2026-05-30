package com.caicai.water;

import com.caicai.common.AppException;
import com.caicai.user.User;
import com.caicai.user.UserRepository;
import com.caicai.water.WaterDtos.CreateWaterRequest;
import com.caicai.water.WaterDtos.WaterSummaryResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WaterServiceTest {

    @Mock private WaterLogRepository waterLogRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private WaterService service;

    // -------------------------------------------------------------------------
    // getByDate — total aggregation
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("getByDate: sums all entries into totalMl")
    void getByDate_sumsTotalMl() {
        LocalDate date = LocalDate.of(2024, 1, 15);

        when(waterLogRepository.findByUserIdAndDate(1L, date)).thenReturn(List.of(
                buildWaterLog(1L, 1L, 250),
                buildWaterLog(2L, 1L, 500),
                buildWaterLog(3L, 1L, 750)
        ));

        WaterSummaryResponse result = service.getByDate(1L, date);

        assertThat(result.getTotalMl()).isEqualTo(1500);
        assertThat(result.getEntries()).hasSize(3);
    }

    @Test
    @DisplayName("getByDate: returns zero total when no entries exist")
    void getByDate_returnsZeroWhenEmpty() {
        LocalDate date = LocalDate.of(2024, 1, 15);

        when(waterLogRepository.findByUserIdAndDate(1L, date)).thenReturn(List.of());

        WaterSummaryResponse result = service.getByDate(1L, date);

        assertThat(result.getTotalMl()).isEqualTo(0);
        assertThat(result.getEntries()).isEmpty();
    }

    @Test
    @DisplayName("getByDate: maps entry amountMl correctly")
    void getByDate_mapsEntryAmountMl() {
        LocalDate date = LocalDate.of(2024, 1, 15);

        when(waterLogRepository.findByUserIdAndDate(1L, date)).thenReturn(List.of(
                buildWaterLog(1L, 1L, 330)
        ));

        WaterSummaryResponse result = service.getByDate(1L, date);

        assertThat(result.getEntries().get(0).getAmountMl()).isEqualTo(330);
    }

    // -------------------------------------------------------------------------
    // delete — ownership
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("delete: succeeds when user owns the log entry")
    void delete_succeedsForOwner() {
        User user = buildUser(1L);
        WaterLog log = buildWaterLog(10L, 1L, 500);
        log.setUser(user);

        when(waterLogRepository.findById(10L)).thenReturn(Optional.of(log));

        assertThatCode(() -> service.delete(1L, 10L)).doesNotThrowAnyException();
        verify(waterLogRepository).delete(log);
    }

    @Test
    @DisplayName("delete: throws FORBIDDEN when user does not own the log entry")
    void delete_throwsForbiddenForNonOwner() {
        User owner = buildUser(1L);
        WaterLog log = buildWaterLog(10L, 1L, 500);
        log.setUser(owner);

        when(waterLogRepository.findById(10L)).thenReturn(Optional.of(log));

        assertThatThrownBy(() -> service.delete(2L, 10L))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));

        verify(waterLogRepository, never()).delete(any());
    }

    @Test
    @DisplayName("delete: throws NOT_FOUND when log entry does not exist")
    void delete_throwsNotFoundWhenLogMissing() {
        when(waterLogRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(1L, 99L))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    // -------------------------------------------------------------------------
    // create — not found cases
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("create: throws NOT_FOUND when user does not exist")
    void create_throwsNotFoundWhenUserMissing() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        CreateWaterRequest dto = new CreateWaterRequest(250, LocalDate.now());

        assertThatThrownBy(() -> service.create(99L, dto))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getStatus()).isEqualTo(HttpStatus.NOT_FOUND));

        verify(waterLogRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private User buildUser(Long id) {
        User user = new User();
        user.setId(id);
        return user;
    }

    private WaterLog buildWaterLog(Long id, Long userId, int amountMl) {
        User user = buildUser(userId);
        WaterLog log = new WaterLog();
        log.setId(id);
        log.setUser(user);
        log.setAmountMl(amountMl);
        log.setLoggedAt(LocalDateTime.now());
        return log;
    }
}
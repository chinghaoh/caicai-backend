package com.caicai.water;

import com.caicai.common.AppException;
import com.caicai.user.User;
import com.caicai.user.UserRepository;
import com.caicai.water.WaterDtos.CreateWaterRequest;
import com.caicai.water.WaterDtos.WaterLogResponse;
import com.caicai.water.WaterDtos.WaterSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WaterService {

    private final WaterLogRepository waterLogRepository;
    private final UserRepository userRepository;

    @Transactional
    public WaterLogResponse create(Long userId, CreateWaterRequest dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "User not found"));

        WaterLog log = WaterLog.builder()
                .user(user)
                .amountMl(dto.getAmountMl())
                .loggedAt(dto.getDate().atStartOfDay())
                .build();

        WaterLog saved = waterLogRepository.save(log);
        return toResponse(saved);
    }

    public WaterSummaryResponse getByDate(Long userId, LocalDate date) {
        List<WaterLog> entries = waterLogRepository.findByUserIdAndDate(userId, date);
        int total = entries.stream().mapToInt(WaterLog::getAmountMl).sum();
        List<WaterLogResponse> responses = entries.stream().map(this::toResponse).toList();
        return WaterSummaryResponse.builder()
                .totalMl(total)
                .entries(responses)
                .build();
    }

    @Transactional
    public void delete(Long userId, Long logId) {
        WaterLog log = waterLogRepository.findById(logId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Water log entry not found"));

        if (!log.getUser().getId().equals(userId)) {
            throw new AppException(HttpStatus.FORBIDDEN, "Not authorised");
        }

        waterLogRepository.delete(log);
    }

    private WaterLogResponse toResponse(WaterLog log) {
        return WaterLogResponse.builder()
                .id(log.getId())
                .amountMl(log.getAmountMl())
                .loggedAt(log.getLoggedAt())
                .build();
    }
}
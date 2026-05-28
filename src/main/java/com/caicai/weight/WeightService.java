package com.caicai.weight;

import com.caicai.common.AppException;
import com.caicai.user.User;
import com.caicai.user.UserRepository;
import com.caicai.weight.WeightDtos.CreateWeightRequest;
import com.caicai.weight.WeightDtos.WeightLogResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WeightService {

    private final WeightLogRepository weightLogRepository;
    private final UserRepository userRepository;

    @Transactional
    public WeightLogResponse create(Long userId, CreateWeightRequest dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "User not found"));

        WeightLog log = WeightLog.builder()
                .user(user)
                .weightKg(dto.getWeightKg())
                .loggedAt(dto.getDate().atStartOfDay())
                .build();

        WeightLog saved = weightLogRepository.save(log);
        return toResponse(saved);
    }

    public List<WeightLogResponse> getAll(Long userId) {
        return weightLogRepository.findByUserIdOrderByLoggedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<WeightLogResponse> getByDateRange(Long userId, LocalDate from, LocalDate to) {
        return weightLogRepository.findByUserIdAndDateBetween(userId, from, to)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void delete(Long userId, Long logId) {
        WeightLog log = weightLogRepository.findById(logId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Weight log entry not found"));

        if (!log.getUser().getId().equals(userId)) {
            throw new AppException(HttpStatus.FORBIDDEN, "Not authorised");
        }

        weightLogRepository.delete(log);
    }

    private WeightLogResponse toResponse(WeightLog log) {
        return WeightLogResponse.builder()
                .id(log.getId())
                .weightKg(log.getWeightKg())
                .loggedAt(log.getLoggedAt())
                .build();
    }
}
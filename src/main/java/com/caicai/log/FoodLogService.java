package com.caicai.log;

import com.caicai.common.AppException;
import com.caicai.food.FoodItem;
import com.caicai.food.FoodItemRepository;
import com.caicai.log.FoodLog.MealType;
import com.caicai.log.FoodLogDtos.CreateFoodLogRequest;
import com.caicai.log.FoodLogDtos.FoodLogResponse;
import com.caicai.user.User;
import com.caicai.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FoodLogService {

    private final FoodLogRepository foodLogRepository;
    private final FoodItemRepository foodItemRepository;
    private final UserRepository userRepository;

    @Transactional
    public FoodLogResponse create(Long userId, CreateFoodLogRequest dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "User not found"));

        FoodItem foodItem = foodItemRepository.findById(dto.getFoodItemId())
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND ,"Food item not found"));

        LocalDateTime loggedAt = dto.getDate().atStartOfDay();

        FoodLog log = FoodLog.builder()
                .user(user)
                .foodItem(foodItem)
                .amountGrams(dto.getAmountGrams())
                .mealType(dto.getMealType())
                .loggedAt(loggedAt)
                .build();

        FoodLog saved = foodLogRepository.save(log);
        return toResponse(saved);
    }

    public List<FoodLogResponse> getByDate(Long userId, LocalDate date) {
        return foodLogRepository.findByUserIdAndDate(userId, date)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void delete(Long userId, Long logId) {
        FoodLog log = foodLogRepository.findById(logId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND,"Log entry not found"));

        if (!log.getUser().getId().equals(userId)) {
            throw new AppException(HttpStatus.FORBIDDEN,"Not authorised");
        }

        foodLogRepository.delete(log);
    }

    private FoodLogResponse toResponse(FoodLog log) {
        FoodItem food = log.getFoodItem();
        double factor = log.getAmountGrams() / 100.0;

        return FoodLogResponse.builder()
                .id(log.getId())
                .foodItemId(food.getId())
                .foodName(food.getName())
                .brand(food.getBrand())
                .amountGrams(log.getAmountGrams())
                .mealType(log.getMealType())
                .loggedAt(log.getLoggedAt())
                .calories(round(food.getCaloriesPer100g().doubleValue() * factor))
                .protein(round(food.getProteinPer100g().doubleValue() * factor))
                .carbs(round(food.getCarbsPer100g().doubleValue() * factor))
                .fat(round(food.getFatPer100g().doubleValue() * factor))
                .build();
    }

    private double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
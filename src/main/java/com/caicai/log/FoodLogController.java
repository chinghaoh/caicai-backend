package com.caicai.log;

import com.caicai.log.FoodLogDtos.CreateFoodLogRequest;
import com.caicai.log.FoodLogDtos.FoodLogResponse;
import com.caicai.user.User;
import com.caicai.user.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/food-logs")
@RequiredArgsConstructor
public class FoodLogController {

    private final FoodLogService foodLogService;

    @PostMapping
    public ResponseEntity<FoodLogResponse> create(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CreateFoodLogRequest dto) {
        return ResponseEntity.ok(foodLogService.create(user.getId(), dto));
    }

    @GetMapping
    public ResponseEntity<List<FoodLogResponse>> getByDate(
            @AuthenticationPrincipal User user,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(foodLogService.getByDate(user.getId(), date));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        foodLogService.delete(user.getId(), id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<FoodLogResponse> update(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @Valid @RequestBody FoodLogDtos.UpdateFoodLogRequest dto) {
        return ResponseEntity.ok(foodLogService.update(user.getId(), id, dto));
    }

    @PostMapping("/copy")
    public ResponseEntity<Void> copyDay(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody FoodLogDtos.CopyDayRequest dto) {
        foodLogService.copyDay(user.getId(), dto.getSourceDate(), dto.getTargetDate());
        return ResponseEntity.noContent().build();
    }
}
package com.caicai.goal;

import com.caicai.user.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/goals")
@RequiredArgsConstructor
public class GoalController {

    private final GoalService goalService;

    @PostMapping("/suggest")
    public ResponseEntity<Map<String, Object>> suggest(
            @Valid @RequestBody GoalDtos.SuggestRequest request) {
        GoalDtos.SuggestResponse suggestion = goalService.suggest(request);
        return ResponseEntity.ok(Map.of("data", suggestion));
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createGoal(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody GoalDtos.CreateGoalRequest request) {
        GoalDtos.GoalResponse goal = goalService.createGoal(user, request);
        return ResponseEntity.ok(Map.of("data", goal));
    }

    @GetMapping("/current")
    public ResponseEntity<Map<String, Object>> getCurrentGoal(
            @AuthenticationPrincipal User user) {
        GoalDtos.GoalResponse goal = goalService.getCurrentGoal(user);
        return ResponseEntity.ok(Map.of("data", goal));
    }

    @GetMapping("/history")
    public ResponseEntity<Map<String, Object>> getGoalHistory(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<GoalDtos.GoalResponse> history = goalService.getGoalHistory(user, page, size);
        return ResponseEntity.ok(Map.of(
                "data", history.getContent(),
                "pagination", Map.of(
                        "page", page,
                        "size", size,
                        "totalElements", history.getTotalElements(),
                        "totalPages", history.getTotalPages()
                )
        ));
    }
}
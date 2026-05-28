package com.caicai.food;

import com.caicai.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/foods")
@RequiredArgsConstructor
public class FoodController {

    private final FoodService foodService;

    @GetMapping
    public ResponseEntity<Map<String, List<FoodDtos.FoodItemResponse>>> search(
            @RequestParam String query,
            @AuthenticationPrincipal User user
    ) {
        if (query == null || query.trim().length() < 2) {
            return ResponseEntity.ok(Map.of("data", List.of()));
        }

        List<FoodDtos.FoodItemResponse> results = foodService.search(query.trim(), user.getId());
        return ResponseEntity.ok(Map.of("data", results));
    }
}
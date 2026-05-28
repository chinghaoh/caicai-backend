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

    @GetMapping("/favourites")
    public ResponseEntity<Map<String, List<FoodDtos.FoodItemResponse>>> getFavourites(
            @AuthenticationPrincipal User user
    ) {
        List<FoodDtos.FoodItemResponse> results = foodService.getFavourites(user.getId());
        return ResponseEntity.ok(Map.of("data", results));
    }

    @PostMapping("/{id}/favourite")
    public ResponseEntity<Map<String, String>> addFavourite(
            @PathVariable Long id,
            @AuthenticationPrincipal User user
    ) {
        foodService.addFavourite(id, user.getId());
        return ResponseEntity.ok(Map.of("data", "ok"));
    }

    @DeleteMapping("/{id}/favourite")
    public ResponseEntity<Void> removeFavourite(
            @PathVariable Long id,
            @AuthenticationPrincipal User user
    ) {
        foodService.removeFavourite(id, user.getId());
        return ResponseEntity.noContent().build();
    }
}
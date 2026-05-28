package com.caicai.weight;

import com.caicai.user.User;
import com.caicai.weight.WeightDtos.CreateWeightRequest;
import com.caicai.weight.WeightDtos.WeightLogResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/weight")
@RequiredArgsConstructor
public class WeightController {

    private final WeightService weightService;

    @PostMapping
    public ResponseEntity<WeightLogResponse> create(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CreateWeightRequest dto) {
        return ResponseEntity.ok(weightService.create(user.getId(), dto));
    }

    @GetMapping
    public ResponseEntity<List<WeightLogResponse>> getAll(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(weightService.getAll(user.getId()));
    }

    @GetMapping("/range")
    public ResponseEntity<List<WeightLogResponse>> getByDateRange(
            @AuthenticationPrincipal User user,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(weightService.getByDateRange(user.getId(), from, to));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        weightService.delete(user.getId(), id);
        return ResponseEntity.noContent().build();
    }
}
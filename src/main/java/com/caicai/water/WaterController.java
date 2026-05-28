package com.caicai.water;

import com.caicai.user.User;
import com.caicai.water.WaterDtos.CreateWaterRequest;
import com.caicai.water.WaterDtos.WaterLogResponse;
import com.caicai.water.WaterDtos.WaterSummaryResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/water")
@RequiredArgsConstructor
public class WaterController {

    private final WaterService waterService;

    @PostMapping
    public ResponseEntity<WaterLogResponse> create(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CreateWaterRequest dto) {
        return ResponseEntity.ok(waterService.create(user.getId(), dto));
    }

    @GetMapping
    public ResponseEntity<WaterSummaryResponse> getByDate(
            @AuthenticationPrincipal User user,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(waterService.getByDate(user.getId(), date));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        waterService.delete(user.getId(), id);
        return ResponseEntity.noContent().build();
    }
}
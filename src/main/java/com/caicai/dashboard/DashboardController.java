package com.caicai.dashboard;

import com.caicai.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getDailySummary(
            @AuthenticationPrincipal User user,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        DashboardDtos.DailySummary summary = dashboardService.getDailySummary(user.getId(), date);
        return ResponseEntity.ok(Map.of("data", summary));
    }

    @GetMapping("/weekly")
    public ResponseEntity<Map<String, Object>> getWeeklySummary(
            @AuthenticationPrincipal User user,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<DashboardDtos.DailyDataPoint> points = dashboardService.getWeeklySummary(user.getId(), date);
        return ResponseEntity.ok(Map.of("data", points));
    }

    @GetMapping("/monthly")
    public ResponseEntity<Map<String, Object>> getMonthlySummary(
            @AuthenticationPrincipal User user,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<DashboardDtos.DailyDataPoint> points = dashboardService.getMonthlySummary(user.getId(), date);
        return ResponseEntity.ok(Map.of("data", points));
    }
}
package com.caicai.goal;

import com.caicai.common.AppException;
import com.caicai.user.User;
import com.caicai.user.UserRepository;
import com.caicai.weight.WeightLog;
import com.caicai.weight.WeightLogRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GoalService {

    private final GoalRepository goalRepository;
    private final WeightLogRepository weightLogRepository;
    private final UserRepository userRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${anthropic.api-key}")
    private String anthropicApiKey;

    private static final String ANTHROPIC_API_URL = "https://api.anthropic.com/v1/messages";
    private static final String ANTHROPIC_MODEL = "claude-opus-4-5";

    public GoalDtos.SuggestResponse suggest(GoalDtos.SuggestRequest request) {
        String prompt = buildPrompt(request);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", anthropicApiKey);
        headers.set("anthropic-version", "2023-06-01");

        Map<String, Object> body = Map.of(
                "model", ANTHROPIC_MODEL,
                "max_tokens", 1024,
                "system", "You are a nutrition expert. Return only valid JSON with no preamble, markdown, or code fences.",
                "messages", List.of(Map.of("role", "user", "content", prompt))
        );

        try {
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(ANTHROPIC_API_URL, entity, String.class);

            JsonNode root = objectMapper.readTree(response.getBody());
            String text = root.path("content").get(0).path("text").asText();
            JsonNode suggestion = objectMapper.readTree(text);

            return new GoalDtos.SuggestResponse(
                    suggestion.path("calories").asInt(),
                    suggestion.path("protein").asInt(),
                    suggestion.path("carbs").asInt(),
                    suggestion.path("fat").asInt(),
                    suggestion.path("waterMl").asInt(),
                    suggestion.path("explanation").asText()
            );
        } catch (Exception e) {
            log.error("AI goal suggestion failed", e);
            throw new AppException(HttpStatus.SERVICE_UNAVAILABLE, "Goal suggestion is temporarily unavailable. Please set your goals manually.");
        }
    }

    private String buildPrompt(GoalDtos.SuggestRequest req) {
        return String.format(
                "Age: %d, weight: %.1fkg, height: %dcm, gender: %s, activity: %s, goal: %s, target weight: %.1fkg. " +
                        "Return JSON: { \"calories\": <int>, \"protein\": <int>, \"carbs\": <int>, \"fat\": <int>, \"waterMl\": <int>, \"explanation\": \"<warm 1-2 sentence explanation>\" }",
                req.age(), req.weightKg(), req.heightCm(),
                req.gender(), req.activityLevel(), req.goalType(),
                req.targetWeightKg()
        );
    }


    @Transactional
    public GoalDtos.GoalResponse createGoal(User user, GoalDtos.CreateGoalRequest request) {
        Goal goal = Goal.builder()
                .user(user)
                .calories(request.calories())
                .protein(request.protein())
                .carbs(request.carbs())
                .fat(request.fat())
                .waterMl(request.waterMl())
                .startingWeightKg(request.startingWeightKg())
                .targetWeightKg(request.targetWeightKg())
                .effectiveFrom(LocalDate.now())
                .build();

        goalRepository.save(goal);

        WeightLog weightLog = WeightLog.builder()
                .user(user)
                .weightKg(request.startingWeightKg())
                .loggedAt(LocalDateTime.now())
                .build();

        weightLogRepository.save(weightLog);

        user.setHasCompletedOnboarding(true);
        userRepository.save(user);

        return toGoalResponse(goal);
    }

    public GoalDtos.GoalResponse getCurrentGoal(User user) {
        Goal goal = goalRepository.findActiveGoalForDate(user.getId(), LocalDate.now())
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "No active goal found"));
        return toGoalResponse(goal);
    }

    public Page<GoalDtos.GoalResponse> getGoalHistory(User user, int page, int size) {
        PageRequest pageable = PageRequest.of(page - 1, size); // 1-indexed → 0-indexed
        return goalRepository.findByUserIdOrderByEffectiveFromDesc(user.getId(), pageable)
                .map(this::toGoalResponse);
    }

    private GoalDtos.GoalResponse toGoalResponse(Goal goal) {
        return new GoalDtos.GoalResponse(
                goal.getId(),
                goal.getCalories(),
                goal.getProtein(),
                goal.getCarbs(),
                goal.getFat(),
                goal.getWaterMl(),
                goal.getStartingWeightKg(),
                goal.getTargetWeightKg(),
                goal.getEffectiveFrom().toString()
        );
    }
}
package com.caicai.food;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@Component
public class FatSecretClient {

    private static final String BASE_URL = "https://platform.fatsecret.com/rest/server.api";

    private final String consumerId;
    private final String consumerSecret;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public FatSecretClient(
            @Value("${fatsecret.id}") String consumerId,
            @Value("${fatsecret.secret}") String consumerSecret) {
        this.consumerId = consumerId;
        this.consumerSecret = consumerSecret;
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    // --- Public API ---

    public List<FatSecretFood> search(String query, int maxResults) {
        try {
            Map<String, String> params = new LinkedHashMap<>();
            params.put("format", "json");
            params.put("max_results", String.valueOf(maxResults));
            params.put("method", "foods.search");
            params.put("search_expression", query);

            String response = execute(params);
            return parseSearchResults(response);
        } catch (Exception e) {
            log.error("FatSecret search failed for query '{}': {}", query, e.getMessage());
            return List.of();
        }
    }

    public Optional<FatSecretFood> getFood(String foodId) {
        try {
            Map<String, String> params = new LinkedHashMap<>();
            params.put("food_id", foodId);
            params.put("format", "json");
            params.put("method", "food.get");

            String response = execute(params);
            return parseFoodGet(response);
        } catch (Exception e) {
            log.error("FatSecret food.get failed for id '{}': {}", foodId, e.getMessage());
            return Optional.empty();
        }
    }

    // --- Parsing ---

    private List<FatSecretFood> parseSearchResults(String json) throws Exception {
        JsonNode root = objectMapper.readTree(json);
        JsonNode foods = root.path("foods").path("food");

        List<FatSecretFood> results = new ArrayList<>();

        if (foods.isArray()) {
            for (JsonNode food : foods) {
                parseSearchFood(food).ifPresent(results::add);
            }
        } else if (foods.isObject()) {
            parseSearchFood(foods).ifPresent(results::add);
        }

        return results;
    }

    private Optional<FatSecretFood> parseSearchFood(JsonNode food) {
        String foodId = food.path("food_id").asText();
        String foodName = food.path("food_name").asText();
        String foodType = food.path("food_type").asText();
        String brandName = food.path("brand_name").asText(null);
        String description = food.path("food_description").asText();

        // Only parse if description is per grams — e.g. "Per 100g", "Per 101g", "Per 85g"
        // Skip "Per 1 serving", "Per 4 oz", etc — those need food.get
        if (!description.matches("Per \\d+g.*") && !description.matches("Per \\d+ g.*")) {
            log.info("Food '{}' (id: {}) needs detail call — description: '{}'", foodName, foodId, description);
            return Optional.of(new FatSecretFood(foodId, foodName, brandName, foodType, null, null, null, null, null, null, null, true));
        }

        try {
            // Extract the gram amount from "Per 101g"
            int perGrams = Integer.parseInt(description.replaceAll("Per (\\d+)\\s*g.*", "$1"));

            BigDecimal calories = extractValue(description, "Calories: ", "kcal");
            BigDecimal fat      = extractValue(description, "Fat: ", "g");
            BigDecimal carbs    = extractValue(description, "Carbs: ", "g");
            BigDecimal protein  = extractValue(description, "Protein: ", "g");

            // Normalize to per 100g if needed
            if (perGrams != 100) {
                BigDecimal factor = new BigDecimal("100").divide(new BigDecimal(perGrams), 4, RoundingMode.HALF_UP);
                calories = calories.multiply(factor).setScale(2, RoundingMode.HALF_UP);
                fat      = fat.multiply(factor).setScale(2, RoundingMode.HALF_UP);
                carbs    = carbs.multiply(factor).setScale(2, RoundingMode.HALF_UP);
                protein  = protein.multiply(factor).setScale(2, RoundingMode.HALF_UP);
            }

            return Optional.of(new FatSecretFood(foodId, foodName, brandName, foodType, calories, protein, carbs, fat, null, null, null, false));
        } catch (Exception e) {
            log.warn("Failed to parse description for food '{}' (id: {}) — description: '{}'", foodName, foodId, description);
            return Optional.of(new FatSecretFood(foodId, foodName, brandName, foodType, null, null, null, null, null, null, null, true));
        }
    }

    private Optional<FatSecretFood> parseFoodGet(String json) throws Exception {
        JsonNode root = objectMapper.readTree(json);
        JsonNode food = root.path("food");

        if (food.isMissingNode()) return Optional.empty();

        String foodId = food.path("food_id").asText();
        String foodName = food.path("food_name").asText();
        String foodType = food.path("food_type").asText();
        String brandName = food.path("brand_name").asText(null);

        JsonNode servings = food.path("servings").path("serving");
        JsonNode per100gServing = find100gServing(servings);

        if (per100gServing == null) {
            log.warn("Skipping food '{}' (id: {}) — no gram-based serving found", foodName, foodId);
            return Optional.empty();
        }

        BigDecimal metricAmount = new BigDecimal(per100gServing.path("metric_serving_amount").asText("100"));

        BigDecimal calories = normalize(per100gServing, "calories", metricAmount);
        BigDecimal protein  = normalize(per100gServing, "protein", metricAmount);
        BigDecimal carbs    = normalize(per100gServing, "carbohydrate", metricAmount);
        BigDecimal fat      = normalize(per100gServing, "fat", metricAmount);
        BigDecimal fiber    = normalize(per100gServing, "fiber", metricAmount);
        BigDecimal sugar    = normalize(per100gServing, "sugar", metricAmount);
        BigDecimal sodium   = normalize(per100gServing, "sodium", metricAmount);

        return Optional.of(new FatSecretFood(foodId, foodName, brandName, foodType, calories, protein, carbs, fat, fiber, sugar, sodium, false));
    }

    private JsonNode find100gServing(JsonNode servings) {
        if (servings.isArray()) {
            for (JsonNode s : servings) {
                String desc = s.path("serving_description").asText("");
                String unit = s.path("metric_serving_unit").asText("");
                if ((desc.equals("100 g") || desc.equals("100g")) && unit.equals("g")) {
                    return s;
                }
            }
            for (JsonNode s : servings) {
                String unit = s.path("metric_serving_unit").asText("");
                if (unit.equals("g")) {
                    return s;
                }
            }
        } else if (servings.isObject()) {
            return servings;
        }
        return null;
    }

    private BigDecimal normalize(JsonNode serving, String field, BigDecimal metricAmount) {
        String raw = serving.path(field).asText("0");
        if (raw.isEmpty() || raw.equals("0")) return BigDecimal.ZERO;
        try {
            BigDecimal value = new BigDecimal(raw);
            if (metricAmount.compareTo(new BigDecimal("100")) == 0) {
                return value.setScale(2, RoundingMode.HALF_UP);
            }
            return value.multiply(new BigDecimal("100"))
                    .divide(metricAmount, 2, RoundingMode.HALF_UP);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private BigDecimal extractValue(String description, String prefix, String suffix) {
        int start = description.indexOf(prefix) + prefix.length();
        int end = description.indexOf(suffix, start);
        return new BigDecimal(description.substring(start, end).trim());
    }

    // --- OAuth 1.0 ---

    private String execute(Map<String, String> params) throws Exception {
        Map<String, String> oauthParams = buildOAuthParams();
        Map<String, String> allParams = new LinkedHashMap<>();
        allParams.putAll(params);
        allParams.putAll(oauthParams);

        String signature = buildSignature(allParams);
        allParams.put("oauth_signature", signature);

        List<Map.Entry<String, String>> entries = new ArrayList<>(allParams.entrySet());
        entries.sort(Map.Entry.comparingByKey());

        StringBuilder body = new StringBuilder();
        for (Map.Entry<String, String> entry : entries) {
            if (body.length() > 0) body.append("&");
            body.append(encode(entry.getKey())).append("=").append(encode(entry.getValue()));
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("FatSecret API error: " + response.statusCode() + " — " + response.body());
        }

        return response.body();
    }

    private Map<String, String> buildOAuthParams() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("oauth_consumer_key", consumerId);
        params.put("oauth_nonce", UUID.randomUUID().toString().replace("-", ""));
        params.put("oauth_signature_method", "HMAC-SHA1");
        params.put("oauth_timestamp", String.valueOf(System.currentTimeMillis() / 1000));
        params.put("oauth_version", "1.0");
        return params;
    }

    private String buildSignature(Map<String, String> params) throws Exception {
        List<Map.Entry<String, String>> entries = new ArrayList<>(params.entrySet());
        entries.sort(Map.Entry.comparingByKey());

        StringBuilder paramString = new StringBuilder();
        for (Map.Entry<String, String> entry : entries) {
            if (paramString.length() > 0) paramString.append("&");
            paramString.append(encode(entry.getKey())).append("=").append(encode(entry.getValue()));
        }

        String baseString = "POST&" + encode(BASE_URL) + "&" + encode(paramString.toString());
        String signingKey = encode(consumerSecret) + "&";

        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(new SecretKeySpec(signingKey.getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
        byte[] raw = mac.doFinal(baseString.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(raw);
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8)
                .replace("+", "%20")
                .replace("*", "%2A")
                .replace("%7E", "~");
    }

    // --- Data record ---

    public record FatSecretFood(
            String foodId,
            String name,
            String brand,
            String foodType,
            BigDecimal caloriesPer100g,
            BigDecimal proteinPer100g,
            BigDecimal carbsPer100g,
            BigDecimal fatPer100g,
            BigDecimal fiberPer100g,
            BigDecimal sugarPer100g,
            BigDecimal sodiumPer100g,
            boolean needsDetailCall
    ) {}
}
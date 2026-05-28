package com.caicai.food;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OpenFoodFactsClient {

    private static final String BASE_URL = "https://world.openfoodfacts.org/cgi/search.pl";
    private static final int PAGE_SIZE = 20;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public List<FoodItem> search(String query) {
        String url = UriComponentsBuilder.fromHttpUrl(BASE_URL)
                .queryParam("search_terms", query)
                .queryParam("search_simple", 1)
                .queryParam("action", "process")
                .queryParam("json", 1)
                .queryParam("page_size", PAGE_SIZE)
                .queryParam("fields", "code,product_name,brands,nutriments")
                .build()
                .toUriString();

        try {
            String raw = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(raw);
            JsonNode products = root.path("products");

            List<FoodItem> results = new ArrayList<>();
            for (JsonNode product : products) {
                FoodItem item = mapProduct(product);
                if (item != null) {
                    results.add(item);
                }
            }
            return results;
        } catch (Exception e) {
            log.error("OpenFoodFacts search failed for query '{}': {}", query, e.getMessage());
            return List.of();
        }
    }

    private FoodItem mapProduct(JsonNode product) {
        String externalId = product.path("code").asText(null);
        String name = product.path("product_name").asText(null);

        if (externalId == null || externalId.isBlank() || name == null || name.isBlank()) {
            return null;
        }

        JsonNode n = product.path("nutriments");

        BigDecimal calories = parseBigDecimal(n, "energy-kcal_100g");
        BigDecimal protein  = parseBigDecimal(n, "proteins_100g");
        BigDecimal carbs    = parseBigDecimal(n, "carbohydrates_100g");
        BigDecimal fat      = parseBigDecimal(n, "fat_100g");

        if (calories == null || protein == null || carbs == null || fat == null) {
            return null;
        }

        String brand = product.path("brands").asText(null);
        if (brand != null && brand.isBlank()) brand = null;

        return FoodItem.builder()
                .name(name.trim())
                .brand(brand != null ? brand.trim() : null)
                .caloriesPer100g(calories)
                .proteinPer100g(protein)
                .carbsPer100g(carbs)
                .fatPer100g(fat)
                .fiberPer100g(parseBigDecimal(n, "fiber_100g"))
                .sugarPer100g(parseBigDecimal(n, "sugars_100g"))
                .sodiumPer100g(parseBigDecimal(n, "sodium_100g"))
                .source(FoodItem.Source.OPENFOODFACTS)
                .externalId(externalId)
                .build();
    }

    private BigDecimal parseBigDecimal(JsonNode node, String field) {
        JsonNode val = node.path(field);
        if (val.isMissingNode() || val.isNull()) return null;
        try {
            return BigDecimal.valueOf(val.asDouble());
        } catch (Exception e) {
            return null;
        }
    }
}
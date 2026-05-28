package com.caicai.food;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FoodService {

    private static final String CACHE_PREFIX = "food_search:";
    private static final Duration CACHE_TTL  = Duration.ofHours(24);

    private final FoodItemRepository         foodItemRepository;
    private final UserFavouriteFoodRepository favouriteRepository;
    private final OpenFoodFactsClient        offClient;
    private final StringRedisTemplate        redisTemplate;

    @Transactional
    public List<FoodDtos.FoodItemResponse> search(String query, Long userId) {
        String cacheKey = CACHE_PREFIX + query.toLowerCase().trim();

        if (!isCached(cacheKey)) {
            List<FoodItem> offResults = offClient.search(query);
            persistNewItems(offResults);
            cacheQuery(cacheKey, query);
        }

        List<FoodItem> dbResults = foodItemRepository.searchByName(query);

        Set<Long> favouriteIds = favouriteRepository
                .findFoodItemsByUserId(userId)
                .stream()
                .map(FoodItem::getId)
                .collect(Collectors.toSet());

        List<FoodItem> favourites    = new ArrayList<>();
        List<FoodItem> nonFavourites = new ArrayList<>();
        for (FoodItem item : dbResults) {
            if (favouriteIds.contains(item.getId())) {
                favourites.add(item);
            } else {
                nonFavourites.add(item);
            }
        }

        List<FoodItem> ordered = new ArrayList<>(favourites);
        ordered.addAll(nonFavourites);

        return ordered.stream()
                .map(item -> toResponse(item, favouriteIds.contains(item.getId())))
                .toList();
    }

    private boolean isCached(String cacheKey) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(cacheKey));
    }

    private void persistNewItems(List<FoodItem> items) {
        for (FoodItem item : items) {
            if (item.getExternalId() != null &&
                    !foodItemRepository.existsByExternalId(item.getExternalId())) {
                foodItemRepository.save(item);
            }
        }
    }

    private void cacheQuery(String cacheKey, String query) {
        try {
            redisTemplate.opsForValue().set(cacheKey, query, CACHE_TTL);
        } catch (Exception e) {
            log.warn("Failed to write to Redis cache for key '{}': {}", cacheKey, e.getMessage());
        }
    }

    private FoodDtos.FoodItemResponse toResponse(FoodItem item, boolean isFavourite) {
        return new FoodDtos.FoodItemResponse(
                item.getId(),
                item.getName(),
                item.getBrand(),
                item.getCaloriesPer100g(),
                item.getProteinPer100g(),
                item.getCarbsPer100g(),
                item.getFatPer100g(),
                item.getFiberPer100g(),
                item.getSugarPer100g(),
                item.getSodiumPer100g(),
                item.getSource().name(),
                isFavourite
        );
    }
}
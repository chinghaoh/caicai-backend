package com.caicai.food;

import com.caicai.common.AppException;
import com.caicai.user.User;
import com.caicai.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
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

    private final FoodItemRepository          foodItemRepository;
    private final UserFavouriteFoodRepository favouriteRepository;
    private final OpenFoodFactsClient         offClient;
    private final StringRedisTemplate         redisTemplate;
    private final UserRepository              userRepository;

    @Transactional
    public List<FoodDtos.FoodItemResponse> search(String query, Long userId) {
        String cacheKey = CACHE_PREFIX + query.toLowerCase().trim();

        if (!isCached(cacheKey)) {
            List<FoodItem> offResults = offClient.search(query);
            if (!offResults.isEmpty()) {
                persistNewItems(offResults);
                cacheQuery(cacheKey, query);
            } else {
                log.warn("OpenFoodFacts returned no results for query '{}', skipping cache", query);
            }
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

    public List<FoodDtos.FoodItemResponse> getFavourites(Long userId) {
        List<FoodItem> items = favouriteRepository.findFoodItemsByUserId(userId);
        return items.stream()
                .map(item -> toResponse(item, true))
                .toList();
    }

    @Transactional
    public void addFavourite(Long foodId, Long userId) {
        if (favouriteRepository.existsByUserIdAndFoodItemId(userId, foodId)) {
            return;
        }

        FoodItem foodItem = foodItemRepository.findById(foodId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Food item not found with id: " + foodId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "User not found"));

        UserFavouriteFood favourite = new UserFavouriteFood(user, foodItem);
        favouriteRepository.save(favourite);
    }

    @Transactional
    public void removeFavourite(Long foodId, Long userId) {
        if (!favouriteRepository.existsByUserIdAndFoodItemId(userId, foodId)) {
            throw new AppException(HttpStatus.NOT_FOUND, "Favourite not found");
        }
        favouriteRepository.deleteByUserIdAndFoodItemId(userId, foodId);
    }

    private boolean isCached(String cacheKey) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(cacheKey));
    }

    private void persistNewItems(List<FoodItem> items) {
        int saved = 0;
        for (FoodItem item : items) {
            if (item.getExternalId() != null &&
                    !foodItemRepository.existsByExternalId(item.getExternalId())) {
                foodItemRepository.save(item);
                saved++;
            }
        }
        log.info("Persisted {} new food items to database", saved);
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
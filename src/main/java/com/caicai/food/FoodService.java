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
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FoodService {

    private static final String CACHE_PREFIX = "fatsecret_search:";
    private static final Duration CACHE_TTL  = Duration.ofHours(24);
    private static final int MAX_RESULTS     = 20;

    private final FoodItemRepository          foodItemRepository;
    private final UserFavouriteFoodRepository favouriteRepository;
    private final FatSecretClient             fatSecretClient;
    private final StringRedisTemplate         redisTemplate;
    private final UserRepository              userRepository;

    @Transactional
    public List<FoodDtos.FoodItemResponse> search(String query, Long userId) {
        String cacheKey = CACHE_PREFIX + query.toLowerCase().trim();

        if (!isCached(cacheKey)) {
            List<FatSecretClient.FatSecretFood> results = fatSecretClient.search(query, MAX_RESULTS);

            if (!results.isEmpty()) {
                persistNewItems(results);
                cacheQuery(cacheKey, query);
            } else {
                log.warn("FatSecret returned no results for query '{}', skipping cache", query);
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



    // --- Private helpers ---

    private void persistNewItems(List<FatSecretClient.FatSecretFood> results) {
        int saved = 0;
        for (FatSecretClient.FatSecretFood result : results) {
            if (foodItemRepository.existsByExternalId(result.foodId())) {
                continue;
            }

            FatSecretClient.FatSecretFood food = result;

            // If search didn't return per-100g data, call food.get for full details
            if (result.needsDetailCall()) {
                Optional<FatSecretClient.FatSecretFood> detail = fatSecretClient.getFood(result.foodId());
                if (detail.isEmpty()) {
                    log.warn("Skipping food {} — food.get returned no usable data", result.foodId());
                    continue;
                }
                food = detail.get();
            }

            // Skip if macros are still null after detail call
            if (food.caloriesPer100g() == null || food.proteinPer100g() == null
                    || food.carbsPer100g() == null || food.fatPer100g() == null) {
                log.warn("Skipping food {} — missing required macro data", result.foodId());
                continue;
            }

            FoodItem item = FoodItem.builder()
                    .name(food.name())
                    .brand(food.brand())
                    .caloriesPer100g(food.caloriesPer100g())
                    .proteinPer100g(food.proteinPer100g())
                    .carbsPer100g(food.carbsPer100g())
                    .fatPer100g(food.fatPer100g())
                    .fiberPer100g(food.fiberPer100g())
                    .sugarPer100g(food.sugarPer100g())
                    .sodiumPer100g(food.sodiumPer100g())
                    .source(FoodItem.Source.FATSECRET)
                    .externalId(result.foodId())
                    .build();

            foodItemRepository.save(item);
            saved++;
        }
        log.info("Persisted {} new food items to database", saved);
    }

    private boolean isCached(String cacheKey) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(cacheKey));
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
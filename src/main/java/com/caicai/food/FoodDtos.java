package com.caicai.food;

import java.math.BigDecimal;

public class FoodDtos {

    public record FoodItemResponse(
            Long id,
            String name,
            String brand,
            BigDecimal caloriesPer100g,
            BigDecimal proteinPer100g,
            BigDecimal carbsPer100g,
            BigDecimal fatPer100g,
            BigDecimal fiberPer100g,
            BigDecimal sugarPer100g,
            BigDecimal sodiumPer100g,
            String source,
            boolean isFavourite
    ) {}
}
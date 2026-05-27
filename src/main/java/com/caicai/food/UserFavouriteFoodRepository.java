package com.caicai.food;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserFavouriteFoodRepository extends JpaRepository<UserFavouriteFood, UserFavouriteFood.UserFavouriteFoodId> {

    @Query("SELECT uf.foodItem FROM UserFavouriteFood uf WHERE uf.user.id = :userId")
    List<FoodItem> findFoodItemsByUserId(@Param("userId") Long userId);

    boolean existsByUserIdAndFoodItemId(@Param("userId") Long userId, @Param("foodItemId") Long foodItemId);

    void deleteByUserIdAndFoodItemId(@Param("userId") Long userId, @Param("foodItemId") Long foodItemId);
}
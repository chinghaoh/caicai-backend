package com.caicai.food;

import com.caicai.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;

@Entity
@Table(name = "user_favourite_foods")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(UserFavouriteFood.UserFavouriteFoodId.class)
public class UserFavouriteFood {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "food_item_id", nullable = false)
    private FoodItem foodItem;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserFavouriteFoodId implements Serializable {
        private Long user;
        private Long foodItem;
    }
}
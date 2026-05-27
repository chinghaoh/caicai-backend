package com.caicai.log;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface FoodLogRepository extends JpaRepository<FoodLog, Long> {

    @Query("SELECT f FROM FoodLog f WHERE f.user.id = :userId AND DATE(f.loggedAt) = :date ORDER BY f.loggedAt ASC")
    List<FoodLog> findByUserIdAndDate(@Param("userId") Long userId, @Param("date") LocalDate date);

    @Query("SELECT f FROM FoodLog f WHERE f.user.id = :userId AND DATE(f.loggedAt) BETWEEN :from AND :to ORDER BY f.loggedAt ASC")
    List<FoodLog> findByUserIdAndDateBetween(@Param("userId") Long userId, @Param("from") LocalDate from, @Param("to") LocalDate to);
}
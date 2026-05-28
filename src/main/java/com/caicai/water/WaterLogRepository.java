package com.caicai.water;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface WaterLogRepository extends JpaRepository<WaterLog, Long> {

    @Query("SELECT w FROM WaterLog w WHERE w.user.id = :userId AND DATE(w.loggedAt) = :date ORDER BY w.loggedAt ASC")
    List<WaterLog> findByUserIdAndDate(@Param("userId") Long userId, @Param("date") LocalDate date);

    @Query("SELECT w FROM WaterLog w WHERE w.user.id = :userId AND DATE(w.loggedAt) BETWEEN :from AND :to ORDER BY w.loggedAt ASC")
    List<WaterLog> findByUserIdAndDateBetween(@Param("userId") Long userId, @Param("from") LocalDate from, @Param("to") LocalDate to);
}
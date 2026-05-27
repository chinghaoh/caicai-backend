package com.caicai.weight;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface WeightLogRepository extends JpaRepository<WeightLog, Long> {

    @Query("SELECT w FROM WeightLog w WHERE w.user.id = :userId ORDER BY w.loggedAt DESC")
    List<WeightLog> findByUserIdOrderByLoggedAtDesc(@Param("userId") Long userId);

    @Query("SELECT w FROM WeightLog w WHERE w.user.id = :userId AND DATE(w.loggedAt) BETWEEN :from AND :to ORDER BY w.loggedAt ASC")
    List<WeightLog> findByUserIdAndDateBetween(@Param("userId") Long userId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("SELECT w FROM WeightLog w WHERE w.user.id = :userId ORDER BY w.loggedAt DESC LIMIT 1")
    Optional<WeightLog> findLatestByUserId(@Param("userId") Long userId);
}
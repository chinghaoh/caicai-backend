package com.caicai.goal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface GoalRepository extends JpaRepository<Goal, Long> {

    @Query("SELECT g FROM Goal g WHERE g.user.id = :userId AND g.effectiveFrom <= :date ORDER BY g.effectiveFrom DESC LIMIT 1")
    Optional<Goal> findActiveGoalForDate(@Param("userId") Long userId, @Param("date") LocalDate date);

    @Query("SELECT g FROM Goal g WHERE g.user.id = :userId ORDER BY g.effectiveFrom DESC")
    Page<Goal> findByUserIdOrderByEffectiveFromDesc(@Param("userId") Long userId, Pageable pageable);
}
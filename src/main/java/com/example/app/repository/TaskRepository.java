package com.example.app.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.app.entity.Task;

public interface TaskRepository extends JpaRepository<Task, Long> {
    @Query("SELECT t FROM Task t WHERE t.id BETWEEN :startId AND :endId")
    List<Task> findByIdBetween(@Param("startId") Long startId, @Param("endId") Long endId);

    List<Task> findByIdLessThanEqual(Long id);

    java.util.Optional<Task> findByName(String name);
}
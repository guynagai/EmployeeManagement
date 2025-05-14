package com.example.app.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.app.entity.Task;

public interface TaskRepository extends JpaRepository<Task, Long> {
    Optional<Task> findByName(String name);
}
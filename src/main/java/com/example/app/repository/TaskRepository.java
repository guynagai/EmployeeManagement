package com.example.app.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.app.entity.Task;

public interface TaskRepository extends JpaRepository<Task, Long> {
    // ID の昇順で全タスクを取得
    List<Task> findAllByOrderByIdAsc();
    // 名前でタスクを検索
    Optional<Task> findByName(String name);
    // ID が指定値以下のタスクを取得
    List<Task> findByIdLessThanEqual(Long id);
}
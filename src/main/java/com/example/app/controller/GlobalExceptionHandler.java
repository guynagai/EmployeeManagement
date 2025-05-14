package com.example.app.controller;

import java.time.LocalDate;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.example.app.repository.TaskRepository;

@ControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @Autowired
    private TaskRepository taskRepository;

    @ExceptionHandler(Exception.class)
    public String handleException(Exception ex, Model model) {
        logger.error("Unhandled exception: {}", ex.getMessage(), ex);
        model.addAttribute("error", "サーバーエラーが発生しました: " + ex.getMessage());
        model.addAttribute("workDate", LocalDate.now());
        model.addAttribute("workplaces", Arrays.asList("ハウス1", "ハウス2", "ハウス3", "ハウス4", "選果場"));
        try {
            model.addAttribute("tasks", taskRepository.findAllByOrderByIdAsc());
        } catch (Exception e) {
            logger.error("Failed to load tasks in exception handler: {}", e.getMessage(), e);
            model.addAttribute("error", model.getAttribute("error") + "; タスクの取得に失敗しました: " + e.getMessage());
        }
        return "employees/shift_assignment_form";
    }
}
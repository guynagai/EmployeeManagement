package com.example.app.controller;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.app.entity.PartTimeEmployee;
import com.example.app.entity.PreferredShift;
import com.example.app.repository.PartTimeEmployeeRepository;
import com.example.app.repository.PreferredShiftRepository;
import com.example.app.repository.TaskRepository;

@Controller
@RequestMapping("/admin/shifts")
public class ShiftController {
    private static final Logger logger = LoggerFactory.getLogger(ShiftController.class);

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private PreferredShiftRepository preferredShiftRepository;

    @Autowired
    private PartTimeEmployeeRepository partTimeEmployeeRepository;

    @GetMapping("/assign")
    public String showAssignmentForm(
            @RequestParam(value = "workDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate workDate,
            Model model) {
        logger.info("Showing shift assignment form for workDate: {}", workDate);
        model.addAttribute("workDate", workDate != null ? workDate : LocalDate.now());
        model.addAttribute("workplaces", Arrays.asList("ハウス1", "ハウス2", "ハウス3", "ハウス4", "選果場"));
        try {
            model.addAttribute("tasks", taskRepository.findAllByOrderByIdAsc());
        } catch (Exception e) {
            logger.error("Failed to load tasks: {}", e.getMessage(), e);
            model.addAttribute("error", "タスクの取得に失敗しました: " + e.getMessage());
        }
        return "employees/shift_assignment_form";
    }

    @PostMapping("/assign")
    public String assignShifts(
            @RequestParam(value = "workDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate workDate,
            @RequestParam Map<String, String> allParams,
            Model model) {
        try {
            if (workDate == null) {
                logger.warn("workDate parameter is missing or invalid, defaulting to today");
                workDate = LocalDate.now();
                model.addAttribute("error", "作業日が指定されていません。今日の日付を使用します。");
            }
            logger.info("Processing shift assignment for workDate: {}", workDate);
            logger.debug("Raw form parameters: {}", allParams);

            Map<Long, Map<String, Object>> workplaceAssignments = new HashMap<>();
            for (Map.Entry<String, String> entry : allParams.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                logger.debug("Processing key: {}, value: {}", key, value);

                // 人数の処理
                if (key.startsWith("assignments[")) {
                    Matcher countMatcher = Pattern.compile("assignments\\[(\\d+)\\]\\[(\\w+)\\]\\[count\\]").matcher(key);
                    if (countMatcher.matches()) {
                        Long workplaceId = Long.parseLong(countMatcher.group(1));
                        String timeSlot = countMatcher.group(2);
                        logger.debug("Parsed count: workplaceId={}, timeSlot={}, count={}", workplaceId, timeSlot, value);

                        Map<String, Object> workplaceData = workplaceAssignments.computeIfAbsent(workplaceId, k -> new HashMap<>());
                        try {
                            int count = value.isEmpty() || value.trim().isEmpty() ? 0 : Integer.parseInt(value.trim());
                            workplaceData.put(timeSlot + "_count", count);
                        } catch (NumberFormatException e) {
                            logger.error("Invalid count value for workplaceId={}, timeSlot={}: {}", workplaceId, timeSlot, value);
                            model.addAttribute("error", "人数の入力値が無効です: " + value);
                            return setupFormModel(model, workDate);
                        }
                    }
                }

                // タスクの処理
                if (key.startsWith("assignments[") && key.endsWith("[tasks][]")) {
                    Matcher taskMatcher = Pattern.compile("assignments\\[(\\d+)\\]\\[tasks\\]\\[\\]").matcher(key);
                    if (taskMatcher.matches()) {
                        Long workplaceId = Long.parseLong(taskMatcher.group(1));
                        logger.debug("Parsed task: workplaceId={}, taskId={}", workplaceId, value);

                        Map<String, Object> workplaceData = workplaceAssignments.computeIfAbsent(workplaceId, k -> new HashMap<>());
                        List<String> taskIds = workplaceData.containsKey("taskIds")
                                ? (List<String>) workplaceData.get("taskIds")
                                : new ArrayList<>();
                        if (!value.isEmpty()) {
                            try {
                                Long taskId = Long.parseLong(value);
                                if (taskRepository.findById(taskId).isEmpty()) {
                                    logger.error("Task not found for taskId={}", taskId);
                                    model.addAttribute("error", "タスクが存在しません: ID=" + taskId);
                                    return setupFormModel(model, workDate);
                                }
                                taskIds.add(value);
                            } catch (NumberFormatException e) {
                                logger.error("Invalid task ID for workplaceId={}: {}", workplaceId, value);
                                model.addAttribute("error", "タスクIDが無効です: " + value);
                                return setupFormModel(model, workDate);
                            }
                        }
                        workplaceData.put("taskIds", taskIds);
                        List<String> taskNames = taskIds.stream()
                                .map(taskId -> {
                                    try {
                                        return taskRepository.findById(Long.parseLong(taskId))
                                                .map(task -> task.getName())
                                                .orElse("");
                                    } catch (NumberFormatException e) {
                                        logger.warn("Invalid task ID: {}", taskId);
                                        return "";
                                    }
                                })
                                .filter(name -> !name.isEmpty())
                                .collect(Collectors.toList());
                        workplaceData.put("tasks", taskNames);
                    }
                }
            }

            // 出勤予定社員を取得
            PreferredShift.DayOfWeek dayOfWeek;
            try {
                dayOfWeek = PreferredShift.DayOfWeek.valueOf(
                        workDate.getDayOfWeek().toString().toUpperCase(Locale.ENGLISH));
            } catch (IllegalArgumentException e) {
                logger.warn("No shifts available for day: {}", workDate.getDayOfWeek());
                dayOfWeek = null;
            }
            List<PreferredShift> preferredShifts = dayOfWeek != null
                    ? preferredShiftRepository.findByDayOfWeek(dayOfWeek)
                    : new ArrayList<>();
            logger.debug("Found {} preferred shifts for day: {}", preferredShifts.size(), dayOfWeek);

            Map<Long, PartTimeEmployee> employees = partTimeEmployeeRepository.findAll().stream()
                    .collect(Collectors.toMap(PartTimeEmployee::getId, employee -> employee));
            logger.debug("Found {} employees", employees.size());

            // 社員ごとのシフト情報を整理
            Map<Long, Map<String, Object>> availableEmployees = new HashMap<>();
            for (PreferredShift shift : preferredShifts) {
                Long employeeId = shift.getEmployeeId();
                PartTimeEmployee employee = employees.get(employeeId);
                if (employee == null) {
                    logger.warn("Employee not found for ID: {}", employeeId);
                    continue;
                }
                Map<String, Object> employeeData = availableEmployees.computeIfAbsent(employeeId, k -> new HashMap<>());
                employeeData.put("name", employee.getNameKanji());
                employeeData.put("skillLevel", employee.getSkillLevel() != null ? employee.getSkillLevel().toString() : "UNKNOWN");
                List<String> timeSlots = employeeData.containsKey("timeSlots")
                        ? (List<String>) employeeData.get("timeSlots")
                        : new ArrayList<>();
                timeSlots.add(shift.getTimeSlot().toString());
                employeeData.put("timeSlots", timeSlots);
            }

            // 人数集計
            Map<String, Long> employeeCounts = new HashMap<>();
            employeeCounts.put("leader", availableEmployees.values().stream()
                    .filter(e -> "LEADER".equals(e.get("skillLevel")))
                    .count());
            employeeCounts.put("general", availableEmployees.values().stream()
                    .filter(e -> "GENERAL".equals(e.get("skillLevel")))
                    .count());
            employeeCounts.put("newcomer", availableEmployees.values().stream()
                    .filter(e -> "NEWCOMER".equals(e.get("skillLevel")))
                    .count());
            employeeCounts.put("amTotal", availableEmployees.values().stream()
                    .filter(e -> ((List<String>) e.get("timeSlots")).contains("AM"))
                    .count());
            employeeCounts.put("pmTotal", availableEmployees.values().stream()
                    .filter(e -> ((List<String>) e.get("timeSlots")).contains("PM"))
                    .count());

            logger.debug("Available employees: {}, Counts: {}", availableEmployees, employeeCounts);
            model.addAttribute("workDate", workDate);
            model.addAttribute("formData", allParams);
            model.addAttribute("assignments", workplaceAssignments);
            model.addAttribute("availableEmployees", availableEmployees);
            model.addAttribute("employeeCounts", employeeCounts);
            model.addAttribute("workplaces", Arrays.asList("ハウス1", "ハウス2", "ハウス3", "ハウス4", "選果場"));
            return "employees/shift_assignment_result";

        } catch (Exception e) {
            logger.error("Failed to process shift assignment: {}", e.getMessage(), e);
            model.addAttribute("error", "シフト割り当て処理中にエラーが発生しました: " + e.getMessage());
            return setupFormModel(model, workDate != null ? workDate : LocalDate.now());
        }
    }

    private String setupFormModel(Model model, LocalDate workDate) {
        model.addAttribute("workDate", workDate);
        model.addAttribute("workplaces", Arrays.asList("ハウス1", "ハウス2", "ハウス3", "ハウス4", "選果場"));
        try {
            model.addAttribute("tasks", taskRepository.findAllByOrderByIdAsc());
        } catch (Exception e) {
            logger.error("Failed to load tasks in error handling: {}", e.getMessage(), e);
            model.addAttribute("error", model.containsAttribute("error") 
                    ? model.getAttribute("error") + "; タスクの取得に失敗しました: " + e.getMessage()
                    : "タスクの取得に失敗しました: " + e.getMessage());
        }
        return "employees/shift_assignment_form";
    }
}
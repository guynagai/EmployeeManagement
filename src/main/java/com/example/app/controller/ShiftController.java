package com.example.app.controller;

import java.time.LocalDate;
import java.util.ArrayList;
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

import com.example.app.dto.ShiftAssignmentDto;
import com.example.app.entity.PartTimeEmployee;
import com.example.app.entity.PreferredShift;
import com.example.app.entity.Workplace;
import com.example.app.repository.PartTimeEmployeeRepository;
import com.example.app.repository.PreferredShiftRepository;
import com.example.app.repository.TaskRepository;
import com.example.app.repository.WorkplaceRepository;
import com.example.app.service.ShiftAssignmentService;

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

    @Autowired
    private WorkplaceRepository workplaceRepository;

    @Autowired
    private ShiftAssignmentService shiftAssignmentService;

    @GetMapping("/assign")
    public String showAssignmentForm(
            @RequestParam(value = "workDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate workDate,
            Model model) {
        logger.info("Showing shift assignment form for workDate: {}", workDate);
        model.addAttribute("workDate", workDate != null ? workDate : LocalDate.now());
        try {
            model.addAttribute("workplaces", workplaceRepository.findAll());
            model.addAttribute("houseTasks", taskRepository.findByIdLessThanEqual(10L));
            model.addAttribute("sortingTask", taskRepository.findByName("選果").orElse(null));
        } catch (Exception e) {
            logger.error("Failed to load workplaces or tasks: {}", e.getMessage(), e);
            model.addAttribute("error", "職場またはタスクの取得に失敗しました: " + e.getMessage());
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

            Map<Long, String> workplaceNames = workplaceRepository.findAll().stream()
                    .collect(Collectors.toMap(Workplace::getId, Workplace::getName));
            Map<Long, Map<String, Object>> formAssignments = new HashMap<>();

            // 未入力の職場をデフォルト0で初期化
            for (Workplace workplace : workplaceRepository.findAll()) {
                Map<String, Object> workplaceData = new HashMap<>();
                workplaceData.put("am_count", 0);
                workplaceData.put("pm_count", 0);
                workplaceData.put("tasks", new ArrayList<String>());
                workplaceData.put("taskIds", new ArrayList<Long>());
                formAssignments.put(workplace.getId(), workplaceData);
                logger.debug("Initialized workplaceId={} with default am_count=0, pm_count=0, tasks=[]", workplace.getId());
            }

            // フォームデータを処理
            for (Map.Entry<String, String> entry : allParams.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                logger.debug("Processing key: {}, value: {}", key, value);

                if (key.contains("${workplace.id}")) {
                    logger.error("Unresolved workplace ID in key: {}. Possible template cache issue.", key);
                    model.addAttribute("error", "フォームの職場IDが正しく解決されていません。フォームを確認し、キャッシュをクリアしてください。");
                    return setupFormModel(model, workDate);
                }

                if (key.startsWith("assignments[")) {
                    // 人数の処理
                    Matcher countMatcher = Pattern.compile("assignments\\[(\\d+)\\]\\[(\\w+)\\]\\[count\\]").matcher(key);
                    if (countMatcher.matches()) {
                        Long workplaceId = Long.parseLong(countMatcher.group(1));
                        if (!workplaceNames.containsKey(workplaceId)) {
                            logger.error("Invalid workplaceId: {}", workplaceId);
                            model.addAttribute("error", "無効な職場ID: " + workplaceId);
                            return setupFormModel(model, workDate);
                        }
                        String timeSlot = countMatcher.group(2);
                        Map<String, Object> workplaceData = formAssignments.get(workplaceId);
                        try {
                            int count = value.isEmpty() ? 0 : Integer.parseInt(value.trim());
                            if (count < 0) {
                                logger.error("Negative count value for workplaceId={}, timeSlot={}: {}", workplaceId, timeSlot, count);
                                model.addAttribute("error", "人数は0以上でなければなりません: " + count);
                                return setupFormModel(model, workDate);
                            }
                            workplaceData.put(timeSlot.toLowerCase() + "_count", count);
                            logger.debug("Set {} for workplaceId={}, count={}", timeSlot.toLowerCase() + "_count", workplaceId, count);
                        } catch (NumberFormatException e) {
                            logger.error("Invalid count value for workplaceId={}, timeSlot={}: {}", workplaceId, timeSlot, value);
                            model.addAttribute("error", "人数の入力値が無効です: " + value);
                            return setupFormModel(model, workDate);
                        }
                    }

                    // タスクの処理
                    Matcher taskMatcher = Pattern.compile("assignments\\[(\\d+)\\]\\[tasks\\]\\[\\]").matcher(key);
                    if (taskMatcher.matches()) {
                        Long workplaceId = Long.parseLong(taskMatcher.group(1));
                        if (!workplaceNames.containsKey(workplaceId)) {
                            logger.error("Invalid workplaceId for task: {}", workplaceId);
                            model.addAttribute("error", "無効な職場ID: " + workplaceId);
                            return setupFormModel(model, workDate);
                        }
                        Map<String, Object> workplaceData = formAssignments.get(workplaceId);
                        List<String> taskNames = (List<String>) workplaceData.get("tasks");
                        List<Long> taskIds = (List<Long>) workplaceData.get("taskIds");
                        if (!value.isEmpty()) {
                            try {
                                Long taskId = Long.parseLong(value);
                                taskRepository.findById(taskId).ifPresentOrElse(
                                    task -> {
                                        if (!taskIds.contains(taskId)) {
                                            taskNames.add(task.getName());
                                            taskIds.add(task.getId());
                                            logger.debug("Added taskId={} ({}) for workplaceId={}", taskId, task.getName(), workplaceId);
                                        } else {
                                            logger.debug("TaskId={} already added for workplaceId={}", taskId, workplaceId);
                                        }
                                    },
                                    () -> logger.warn("Task not found for taskId={} for workplaceId={}. Check tasks table.", taskId, workplaceId)
                                );
                            } catch (NumberFormatException e) {
                                logger.error("Invalid task ID for workplaceId={}: {}", workplaceId, value);
                                model.addAttribute("error", "タスクIDが無効です: " + value);
                                return setupFormModel(model, workDate);
                            }
                        }
                    }
                }
            }

            logger.debug("Processed formAssignments: {}", formAssignments);

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
            if (preferredShifts.isEmpty()) {
                logger.warn("No preferred shifts found for dayOfWeek={}", dayOfWeek);
            } else {
                logger.debug("Found {} preferred shifts for dayOfWeek={}", preferredShifts.size(), dayOfWeek);
            }
            Map<Long, PartTimeEmployee> employees = partTimeEmployeeRepository.findAll().stream()
                    .collect(Collectors.toMap(PartTimeEmployee::getId, employee -> employee));

            // 社員割り当て
            Map<Long, ShiftAssignmentDto> assignments = shiftAssignmentService.assignEmployees(
                    workDate, formAssignments, employees, preferredShifts, workplaceNames);

            // 人数集計
            Map<Long, Map<String, Object>> availableEmployees = new HashMap<>();
            for (PreferredShift shift : preferredShifts) {
                Long employeeId = shift.getEmployeeId();
                PartTimeEmployee employee = employees.get(employeeId);
                if (employee == null) {
                    logger.warn("Employee not found for ID: {}", employeeId);
                    continue;
                }
                Map<String, Object> employeeData = availableEmployees.computeIfAbsent(employeeId, k -> new HashMap<>());
                employeeData.putIfAbsent("name", employee.getNameKanji() != null ? employee.getNameKanji() : "不明");
                employeeData.putIfAbsent("skillLevel", employee.getSkillLevel() != null ? employee.getSkillLevel().toString() : "UNKNOWN");
                List<String> timeSlots = (List<String>) employeeData.computeIfAbsent("timeSlots", k -> new ArrayList<>());
                timeSlots.add(shift.getTimeSlot().toString());
            }

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

            model.addAttribute("workDate", workDate);
            model.addAttribute("formData", allParams);
            model.addAttribute("assignments", assignments);
            model.addAttribute("availableEmployees", availableEmployees);
            model.addAttribute("employeeCounts", employeeCounts);
            model.addAttribute("workplaces", workplaceRepository.findAll());
            return "employees/shift_assignment_result";

        } catch (Exception e) {
            logger.error("Failed to process shift assignment: {}", e.getMessage(), e);
            model.addAttribute("error", "シフト割り当て処理中にエラーが発生しました: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            return setupFormModel(model, workDate != null ? workDate : LocalDate.now());
        }
    }

    private String setupFormModel(Model model, LocalDate workDate) {
        model.addAttribute("workDate", workDate);
        try {
            model.addAttribute("workplaces", workplaceRepository.findAll());
            model.addAttribute("houseTasks", taskRepository.findByIdLessThanEqual(10L));
            model.addAttribute("sortingTask", taskRepository.findByName("選果").orElse(null));
        } catch (Exception e) {
            logger.error("Failed to load workplaces or tasks: {}", e.getMessage(), e);
            model.addAttribute("error", model.containsAttribute("error")
                    ? model.getAttribute("error") + "; 職場またはタスクの取得に失敗しました: " + e.getMessage()
                    : "職場またはタスクの取得に失敗しました: " + e.getMessage());
        }
        return "employees/shift_assignment_form";
    }
}
    
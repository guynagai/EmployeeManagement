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
import com.example.app.entity.Task;
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
        logger.debug("Checking repository beans: workplaceRepository={}, taskRepository={}", 
                     workplaceRepository != null, taskRepository != null);
        model.addAttribute("workDate", workDate != null ? workDate : LocalDate.now());
        try {
            logger.debug("Fetching workplaces...");
            List<Workplace> workplaces = workplaceRepository.findAll()
                    .stream()
                    .filter(w -> w != null && w.getId() != null)
                    .collect(Collectors.toList());
            logger.debug("Fetched {} workplaces: {}", workplaces.size(), workplaces);
            for (Workplace wp : workplaces) {
                logger.debug("Workplace: id={}, name={}", wp.getId(), wp.getName());
            }
            model.addAttribute("workplaces", workplaces);

            logger.debug("Fetching first house tasks (id 1-5)...");
            List<Task> firstHouseTasks = taskRepository.findByIdBetween(1L, 5L);
            logger.debug("Fetched {} first house tasks: {}", firstHouseTasks.size(), firstHouseTasks);
            model.addAttribute("firstHouseTasks", firstHouseTasks);

            logger.debug("Fetching second house tasks (id 6-10)...");
            List<Task> secondHouseTasks = taskRepository.findByIdBetween(6L, 10L);
            logger.debug("Fetched {} second house tasks: {}", secondHouseTasks.size(), secondHouseTasks);
            model.addAttribute("secondHouseTasks", secondHouseTasks);
        } catch (Exception e) {
            logger.error("Failed to load workplaces or tasks: {}", e.getMessage(), e);
            model.addAttribute("error", "職場またはタスクの取得に失敗しました: " + e.getMessage());
            return "employees/shift_assignment_form";
        }
        return "employees/shift_assignment_form";
    }

    @PostMapping("/assign")
    public String assignShifts(
            @RequestParam(value = "workDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate workDate,
            @RequestParam Map<String, String> allParams,
            Model model) {
        logger.info("Processing shift assignment for workDate: {}", workDate);
        logger.debug("Raw form parameters: {}", allParams);
        Map<Long, ShiftAssignmentDto> assignments = new HashMap<>();
        List<String> errors = new ArrayList<>();
        try {
            if (workDate == null) {
                logger.warn("workDate parameter is missing or invalid, defaulting to today");
                workDate = LocalDate.now();
                errors.add("作業日が指定されていません。今日の日付を使用します。");
            }

            Map<Long, String> workplaceNames = workplaceRepository.findAll().stream()
                    .filter(w -> w != null && w.getId() != null)
                    .collect(Collectors.toMap(Workplace::getId, Workplace::getName));
            logger.debug("Workplace names: {}", workplaceNames);
            Map<Long, Map<String, Object>> formAssignments = new HashMap<>();

            // 未入力の職場を初期化
            for (Workplace workplace : workplaceRepository.findAll()) {
                if (workplace != null && workplace.getId() != null) {
                    Map<String, Object> workplaceData = new HashMap<>();
                    workplaceData.put("am_count", 0);
                    workplaceData.put("pm_count", 0);
                    workplaceData.put("tasks", new ArrayList<String>());
                    workplaceData.put("taskIds", new ArrayList<Long>());
                    formAssignments.put(workplace.getId(), workplaceData);
                    logger.debug("Initialized workplaceId={} with default am_count=0, pm_count=0, tasks={}", workplace.getId(), workplaceData.get("tasks"));
                }
            }

            // フォームデータを処理
            for (Map.Entry<String, String> entry : allParams.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                logger.debug("Processing key: {}, value: {}", key, value);

                // workplaceIdの処理
                Matcher workplaceIdMatcher = Pattern.compile("assignments\\[(\\d+)\\]\\[workplaceId\\]").matcher(key);
                if (workplaceIdMatcher.matches()) {
                    try {
                        Long workplaceId = Long.parseLong(value.trim());
                        if (!workplaceNames.containsKey(workplaceId)) {
                            logger.error("Invalid workplaceId: {}", workplaceId);
                            errors.add("無効な職場ID: " + workplaceId);
                            continue;
                        }
                        int index = Integer.parseInt(workplaceIdMatcher.group(1));
                        formAssignments.computeIfAbsent(workplaceId, k -> new HashMap<>()).put("index", index);
                        logger.debug("Set workplaceId={} for index={}", workplaceId, index);
                    } catch (NumberFormatException e) {
                        logger.error("Invalid workplaceId value for key={}: {}", key, value, e);
                        errors.add("職場IDが無効です: " + value);
                    }
                }

                // 人数の処理
                Matcher countMatcher = Pattern.compile("assignments\\[(\\d+)\\]\\[(\\w+)\\]\\[count\\]").matcher(key);
                if (countMatcher.matches()) {
                    try {
                        int index = Integer.parseInt(countMatcher.group(1));
                        Long workplaceId = formAssignments.entrySet().stream()
                                .filter(e -> e.getValue().get("index") != null && (int)e.getValue().get("index") == index)
                                .map(Map.Entry::getKey)
                                .findFirst()
                                .orElse(null);
                        if (workplaceId == null || !workplaceNames.containsKey(workplaceId)) {
                            logger.error("No valid workplaceId found for index: {}", index);
                            errors.add("無効なインデックス: " + index);
                            continue;
                        }
                        String timeSlot = countMatcher.group(2);
                        Map<String, Object> workplaceData = formAssignments.get(workplaceId);
                        int count = value.isEmpty() ? 0 : Integer.parseInt(value.trim());
                        if (count < 0) {
                            logger.error("Negative count value for workplaceId={}, timeSlot={}: {}", workplaceId, timeSlot, count);
                            errors.add("人数は0以上でなければなりません: " + count + " (職場ID: " + workplaceId + ")");
                            continue;
                        }
                        workplaceData.put(timeSlot.toLowerCase() + "_count", count);
                        logger.debug("Set {} for workplaceId={}, count={}", timeSlot.toLowerCase() + "_count", workplaceId, count);
                    } catch (NumberFormatException e) {
                        logger.error("Invalid count value for key={}: {}", key, value, e);
                        errors.add("人数の入力値が無効です: " + value);
                    }
                }

                // タスクの処理（選果場は無視）
                Matcher taskMatcher = Pattern.compile("assignments\\[(\\d+)\\]\\[tasks\\]\\[\\]").matcher(key);
                if (taskMatcher.matches()) {
                    try {
                        int index = Integer.parseInt(taskMatcher.group(1));
                        Long workplaceId = formAssignments.entrySet().stream()
                                .filter(e -> e.getValue().get("index") != null && (int)e.getValue().get("index") == index)
                                .map(Map.Entry::getKey)
                                .findFirst()
                                .orElse(null);
                        if (workplaceId == null || !workplaceNames.containsKey(workplaceId)) {
                            logger.error("No valid workplaceId found for task index: {}", index);
                            errors.add("無効なインデックス: " + index);
                            continue;
                        }
                        if (!workplaceNames.get(workplaceId).equals("選果場")) {
                            Map<String, Object> workplaceData = formAssignments.get(workplaceId);
                            List<String> taskNames = (List<String>) workplaceData.computeIfAbsent("tasks", k -> new ArrayList<String>());
                            List<Long> taskIds = (List<Long>) workplaceData.computeIfAbsent("taskIds", k -> new ArrayList<Long>());
                            if (!value.isEmpty()) {
                                final Long taskId = Long.parseLong(value);
                                final Long finalWorkplaceId = workplaceId;
                                taskRepository.findById(taskId).ifPresent(task -> {
                                    if (!taskIds.contains(taskId) && taskId >= 1 && taskId <= 10) {
                                        taskNames.add(task.getName());
                                        taskIds.add(task.getId());
                                        logger.debug("Added taskId={} ({}) for workplaceId={}", taskId, task.getName(), finalWorkplaceId);
                                    }
                                });
                            }
                        }
                    } catch (NumberFormatException e) {
                        logger.error("Invalid task ID for key={}: {}", key, value, e);
                        errors.add("タスクIDが無効です: " + value);
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
                logger.warn("No shifts available for day: {}", workDate.getDayOfWeek(), e);
                dayOfWeek = null;
                errors.add("指定された曜日にシフトがありません: " + workDate.getDayOfWeek());
            }
            List<PreferredShift> preferredShifts = dayOfWeek != null
                    ? preferredShiftRepository.findByDayOfWeek(dayOfWeek)
                    : new ArrayList<>();
            logger.debug("Found {} preferred shifts for dayOfWeek={}: {}", 
                        preferredShifts.size(), dayOfWeek, preferredShifts);
            if (preferredShifts.isEmpty()) {
                logger.warn("No preferred shifts found for dayOfWeek={}", dayOfWeek);
                errors.add("指定された曜日の出勤希望シフトがありません。");
            }
            Map<Long, PartTimeEmployee> employees = partTimeEmployeeRepository.findAll().stream()
                    .filter(e -> e != null && e.getId() != null)
                    .collect(Collectors.toMap(PartTimeEmployee::getId, employee -> employee));
            logger.debug("Found {} employees: {}", employees.size(), employees.keySet());

            // 社員割り当て
            try {
                assignments = shiftAssignmentService.assignEmployees(
                        workDate, formAssignments, employees, preferredShifts, workplaceNames);
            } catch (Exception e) {
                logger.error("Failed to assign employees: {}", e.getMessage(), e);
                errors.add("シフト割り当て中にエラーが発生しました: " + e.getMessage());
            }

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
                List<String> timeSlots = (List<String>) employeeData.computeIfAbsent("timeSlots", k -> new ArrayList<String>());
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

            // モデルにデータをセット
            model.addAttribute("workDate", workDate);
            model.addAttribute("assignments", assignments);
            model.addAttribute("availableEmployees", availableEmployees);
            model.addAttribute("employeeCounts", employeeCounts);
            model.addAttribute("workplaces", workplaceRepository.findAll().stream()
                    .filter(w -> w != null && w.getId() != null)
                    .collect(Collectors.toList()));
            if (!errors.isEmpty()) {
                model.addAttribute("error", String.join("; ", errors));
            }
            logger.debug("Returning assignments: {}", assignments);
            return "employees/shift_assignment_result";
        } catch (Exception e) {
            logger.error("Unexpected error in assignShifts: {}", e.getMessage(), e);
            errors.add("予期しないエラーが発生しました: " + e.getMessage());
            model.addAttribute("error", String.join("; ", errors));
            model.addAttribute("workDate", workDate != null ? workDate : LocalDate.now());
            model.addAttribute("workplaces", workplaceRepository.findAll().stream()
                    .filter(w -> w != null && w.getId() != null)
                    .collect(Collectors.toList()));
            model.addAttribute("firstHouseTasks", taskRepository.findByIdBetween(1L, 5L));
            model.addAttribute("secondHouseTasks", taskRepository.findByIdBetween(6L, 10L));
            return "employees/shift_assignment_form";
        }
    }
}
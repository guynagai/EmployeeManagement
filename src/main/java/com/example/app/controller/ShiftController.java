package com.example.app.controller;

import java.time.DayOfWeek;
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
import com.example.app.service.ShiftService;

@Controller
@RequestMapping("/admin")
@SuppressWarnings({"unchecked", "unused"})
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

    @Autowired
    private ShiftService shiftService;

    private LocalDate getDefaultWorkDate() {
        LocalDate today = LocalDate.now();
        if (today.getDayOfWeek() == DayOfWeek.SATURDAY) {
            return today.plusDays(2); // Saturday -> Monday
        }
        return today.plusDays(1); // Otherwise, next day
    }

    @GetMapping("/menu")
    public String showAdminMenu(Model model) {
        logger.info("Showing admin menu, resolving template: admin_menu");
        return "admin_menu";
    }

    @GetMapping("/shifts/preferred")
    public String showPreferredShifts(
            @RequestParam(value = "workDate", required = false) String workDate,
            Model model) {
        logger.info("Showing preferred shifts for workDate: {}", workDate);
        workDate = workDate != null ? workDate : getDefaultWorkDate().toString();
        model.addAttribute("workDate", workDate);
        try {
            Map<String, Object> result = shiftService.getAvailableEmployees(workDate);
            if (result.containsKey("error")) {
                logger.warn("Error from ShiftService: {}", result.get("error"));
                model.addAttribute("error", result.get("error"));
                return "employees/available_employees";
            }
            List<PartTimeEmployee> employees = (List<PartTimeEmployee>) result.get("employees");
            List<PreferredShift> preferredShifts = (List<PreferredShift>) result.get("preferredShifts");
            model.addAttribute("employees", employees != null ? employees : new ArrayList<>());
            model.addAttribute("preferredShifts", preferredShifts != null ? preferredShifts : new ArrayList<>());
            model.addAttribute("leaderCount", result.getOrDefault("leaderCount", 0L));
            model.addAttribute("generalCount", result.getOrDefault("generalCount", 0L));
            model.addAttribute("newcomerCount", result.getOrDefault("newcomerCount", 0L));
            model.addAttribute("totalCountAM", result.getOrDefault("totalCountAM", 0L));
            model.addAttribute("totalCountPM", result.getOrDefault("totalCountPM", 0L));
            logger.debug("Model attributes: workDate={}, employees.size={}",
                    workDate, employees != null ? employees.size() : 0);
            return "employees/available_employees";
        } catch (Exception e) {
            logger.error("Failed to load preferred shifts: {}", e.getMessage(), e);
            model.addAttribute("error", "出勤予定者の取得に失敗しました: " + e.getMessage());
            return "employees/available_employees";
        }
    }

    @GetMapping("/shifts/assign")
    public String showAssignmentForm(
            @RequestParam(value = "workDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate workDate,
            Model model) {
        logger.info("Showing shift assignment form for workDate: {}", workDate);
        workDate = workDate != null ? workDate : getDefaultWorkDate();
        model.addAttribute("workDate", workDate);
        try {
            List<Workplace> workplaces = workplaceRepository.findAll()
                    .stream()
                    .filter(w -> w != null && w.getId() != null)
                    .collect(Collectors.toList());
            model.addAttribute("workplaces", workplaces);

            List<Task> firstHouseTasks = taskRepository.findByIdBetween(1L, 5L);
            model.addAttribute("firstHouseTasks", firstHouseTasks);

            List<Task> secondHouseTasks = taskRepository.findByIdBetween(6L, 10L);
            model.addAttribute("secondHouseTasks", secondHouseTasks);

            PreferredShift.DayOfWeek dayOfWeek;
            try {
                dayOfWeek = PreferredShift.DayOfWeek.valueOf(
                        workDate.getDayOfWeek().toString().toUpperCase(Locale.ENGLISH));
            } catch (IllegalArgumentException e) {
                logger.warn("No shifts available for day: {}", workDate.getDayOfWeek(), e);
                dayOfWeek = null;
            }
            List<PreferredShift> preferredShifts = dayOfWeek != null
                    ? preferredShiftRepository.findByDayOfWeek(dayOfWeek)
                    : new ArrayList<>();
            Map<Long, PartTimeEmployee> employees = partTimeEmployeeRepository.findAll().stream()
                    .filter(e -> e != null && e.getId() != null)
                    .collect(Collectors.toMap(PartTimeEmployee::getId, e -> e));
            Map<Long, Map<String, Object>> availableEmployees = new HashMap<>();
            for (PreferredShift shift : preferredShifts) {
                Long employeeId = shift.getEmployeeId();
                PartTimeEmployee employee = employees.get(employeeId);
                if (employee == null) continue;
                Map<String, Object> employeeData = availableEmployees.computeIfAbsent(employeeId, k -> new HashMap<>());
                employeeData.putIfAbsent("name", employee.getNameKanji() != null ? employee.getNameKanji() : "不明");
                employeeData.putIfAbsent("skillLevel", employee.getSkillLevel() != null ? employee.getSkillLevel().toString() : "UNKNOWN");
                List<String> timeSlots = (List<String>) employeeData.computeIfAbsent("timeSlots", k -> new ArrayList<>());
                if (shift.getTimeSlot() != null && !timeSlots.contains(shift.getTimeSlot().toString())) {
                    timeSlots.add(shift.getTimeSlot().toString());
                }
            }
            Map<String, Long> employeeCounts = new HashMap<>();
            employeeCounts.put("amTotal", preferredShifts.stream()
                    .filter(shift -> shift.getTimeSlot() != null && shift.getTimeSlot() == PreferredShift.TimeSlot.AM)
                    .map(PreferredShift::getEmployeeId)
                    .filter(id -> id != null)
                    .distinct()
                    .count());
            employeeCounts.put("pmTotal", preferredShifts.stream()
                    .filter(shift -> shift.getTimeSlot() != null && shift.getTimeSlot() == PreferredShift.TimeSlot.PM)
                    .map(PreferredShift::getEmployeeId)
                    .filter(id -> id != null)
                    .distinct()
                    .count());
            model.addAttribute("employeeCounts", employeeCounts);
        } catch (Exception e) {
            logger.error("Failed to load workplaces or tasks: {}", e.getMessage(), e);
            model.addAttribute("error", "職場またはタスクの取得に失敗しました: " + e.getMessage());
            return "employees/shift_assignment_form";
        }
        return "employees/shift_assignment_form";
    }

    @PostMapping("/shifts/assign")
    public String assignShifts(
            @RequestParam(value = "workDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate workDate,
            @RequestParam Map<String, String> allParams,
            Model model) {
        logger.info("Processing shift assignment for workDate: {}", workDate);
        workDate = workDate != null ? workDate : getDefaultWorkDate();
        Map<Long, ShiftAssignmentDto> assignments = new HashMap<>();
        List<String> errors = new ArrayList<>();
        try {
            Map<Long, String> workplaceNames = workplaceRepository.findAll().stream()
                    .filter(w -> w != null && w.getId() != null)
                    .collect(Collectors.toMap(Workplace::getId, Workplace::getName));
            Map<Long, Map<String, Object>> formAssignments = new HashMap<>();

            // Initialize workplaces
            for (Workplace workplace : workplaceRepository.findAll()) {
                if (workplace != null && workplace.getId() != null) {
                    Map<String, Object> workplaceData = new HashMap<>();
                    workplaceData.put("am_count", 0);
                    workplaceData.put("pm_count", 0);
                    workplaceData.put("tasks", new ArrayList<String>());
                    workplaceData.put("taskIds", new ArrayList<Long>());
                    formAssignments.put(workplace.getId(), workplaceData);
                }
            }

            // Process form data
            Map<Integer, Long> indexToWorkplaceId = new HashMap<>();
            for (Map.Entry<String, String> entry : allParams.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();

                // workplaceId
                Matcher workplaceIdMatcher = Pattern.compile("assignments\\[(\\d+)\\]\\[workplaceId\\]").matcher(key);
                if (workplaceIdMatcher.matches()) {
                    try {
                        Long workplaceId = Long.parseLong(value.trim());
                        if (!workplaceNames.containsKey(workplaceId)) {
                            errors.add("無効な職場ID: " + workplaceId);
                            continue;
                        }
                        int index = Integer.parseInt(workplaceIdMatcher.group(1));
                        indexToWorkplaceId.put(index, workplaceId);
                        formAssignments.computeIfAbsent(workplaceId, k -> new HashMap<>()).put("index", index);
                    } catch (NumberFormatException e) {
                        errors.add("職場IDが無効です: " + value);
                    }
                }

                // Counts
                Matcher countMatcher = Pattern.compile("assignments\\[(\\d+)\\]\\[(\\w+)\\]\\[count\\]").matcher(key);
                if (countMatcher.matches()) {
                    try {
                        int index = Integer.parseInt(countMatcher.group(1));
                        Long workplaceId = indexToWorkplaceId.get(index);
                        if (workplaceId == null || !workplaceNames.containsKey(workplaceId)) {
                            errors.add("無効なインデックス: " + index);
                            continue;
                        }
                        String timeSlot = countMatcher.group(2);
                        Map<String, Object> workplaceData = formAssignments.get(workplaceId);
                        int count = value.isEmpty() ? 0 : Integer.parseInt(value.trim());
                        if (count < 0) {
                            errors.add("人数は0以上でなければなりません: " + count + " (職場ID: " + workplaceId + ")");
                            continue;
                        }
                        workplaceData.put(timeSlot.toLowerCase() + "_count", count);
                    } catch (NumberFormatException e) {
                        errors.add("人数の入力値が無効です: " + value);
                    }
                }

                // Tasks
                Matcher taskMatcher = Pattern.compile("assignments\\[(\\d+)\\]\\[tasks\\]\\[\\]").matcher(key);
                if (taskMatcher.matches()) {
                    try {
                        int index = Integer.parseInt(taskMatcher.group(1));
                        Long workplaceId = indexToWorkplaceId.get(index);
                        if (workplaceId == null || !workplaceNames.containsKey(workplaceId)) {
                            errors.add("無効なインデックス: " + index);
                            continue;
                        }
                        if (!workplaceNames.get(workplaceId).equals("選果場")) {
                            Map<String, Object> workplaceData = formAssignments.get(workplaceId);
                            List<String> taskNames = (List<String>) workplaceData.computeIfAbsent("tasks", k -> new ArrayList<>());
                            List<Long> taskIds = (List<Long>) workplaceData.computeIfAbsent("taskIds", k -> new ArrayList<>());
                            if (!value.isEmpty()) {
                                Long taskId = Long.parseLong(value);
                                taskRepository.findById(taskId).ifPresent(task -> {
                                    if (taskId >= 1 && taskId <= 10 && !taskIds.contains(taskId)) {
                                        taskNames.add(task.getName());
                                        taskIds.add(taskId);
                                    }
                                });
                            }
                        }
                    } catch (NumberFormatException e) {
                        errors.add("タスクIDが無効です: " + value);
                    }
                }
            }

            // Get preferred shifts
            PreferredShift.DayOfWeek dayOfWeek;
            try {
                dayOfWeek = PreferredShift.DayOfWeek.valueOf(
                        workDate.getDayOfWeek().toString().toUpperCase(Locale.ENGLISH));
            } catch (IllegalArgumentException e) {
                errors.add("指定された曜日にシフトがありません: " + workDate.getDayOfWeek());
                dayOfWeek = null;
            }
            List<PreferredShift> preferredShifts = dayOfWeek != null
                    ? preferredShiftRepository.findByDayOfWeek(dayOfWeek)
                    : new ArrayList<>();
            if (preferredShifts.isEmpty()) {
                errors.add("指定された曜日の出勤希望シフトがありません。");
            }
            Map<Long, PartTimeEmployee> employees = partTimeEmployeeRepository.findAll().stream()
                    .filter(e -> e != null && e.getId() != null)
                    .collect(Collectors.toMap(PartTimeEmployee::getId, e -> e));

            // Assign employees
            try {
                assignments = shiftAssignmentService.assignEmployees(
                        workDate, formAssignments, employees, preferredShifts, workplaceNames);
            } catch (Exception e) {
                errors.add("シフト割り当て中にエラーが発生しました: " + e.getMessage());
            }

            model.addAttribute("workDate", workDate);
            model.addAttribute("assignments", assignments);
            model.addAttribute("workplaces", workplaceRepository.findAll().stream()
                    .filter(w -> w != null && w.getId() != null)
                    .collect(Collectors.toList()));
            if (!errors.isEmpty()) {
                model.addAttribute("error", String.join("; ", errors));
            }
            return "employees/shift_assignment_result";
        } catch (Exception e) {
            errors.add("予期しないエラーが発生しました: " + e.getMessage());
            model.addAttribute("error", String.join("; ", errors));
            model.addAttribute("workDate", workDate != null ? workDate : getDefaultWorkDate());
            model.addAttribute("workplaces", workplaceRepository.findAll().stream()
                    .filter(w -> w != null && w.getId() != null)
                    .collect(Collectors.toList()));
            model.addAttribute("firstHouseTasks", taskRepository.findByIdBetween(1L, 5L));
            model.addAttribute("secondHouseTasks", taskRepository.findByIdBetween(6L, 10L));
            model.addAttribute("employeeCounts", new HashMap<String, Long>());
            return "employees/shift_assignment_form";
        }
    }
}
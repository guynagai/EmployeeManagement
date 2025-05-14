package com.example.app.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.app.entity.PartTimeEmployee;
import com.example.app.entity.PreferredShift;
import com.example.app.entity.ShiftAssignment;
import com.example.app.entity.Task;
import com.example.app.entity.Workplace;
import com.example.app.repository.PartTimeEmployeeRepository;
import com.example.app.repository.PreferredShiftRepository;
import com.example.app.repository.ShiftAssignmentRepository;
import com.example.app.repository.TaskRepository;
import com.example.app.repository.WorkplaceRepository;

@Service
public class ShiftService {

    private static final Logger logger = LoggerFactory.getLogger(ShiftService.class);

    @Autowired
    private PartTimeEmployeeRepository employeeRepository;

    @Autowired
    private PreferredShiftRepository preferredShiftRepository;

    @Autowired
    private ShiftAssignmentRepository shiftAssignmentRepository;

    @Autowired
    private WorkplaceRepository workplaceRepository;

    @Autowired
    private TaskRepository taskRepository;

    public Map<String, Object> getAvailableEmployees(LocalDate workDate) {
        logger.info("Fetching available employees for workDate: {}", workDate);
        String dayOfWeek = workDate.getDayOfWeek().toString();
        PreferredShift.DayOfWeek targetDay;
        try {
            targetDay = PreferredShift.DayOfWeek.valueOf(dayOfWeek);
        } catch (IllegalArgumentException e) {
            logger.error("Invalid day of week: {}", dayOfWeek, e);
            return Map.of("error", "無効な曜日です: " + dayOfWeek);
        }

        List<PreferredShift> preferredShifts = preferredShiftRepository.findAll().stream()
                .filter(ps -> ps.getDayOfWeek() == targetDay)
                .toList();
        logger.debug("Preferred shifts count: {}", preferredShifts.size());

        List<PartTimeEmployee> employees = employeeRepository.findAll().stream()
                .filter(emp -> preferredShifts.stream().anyMatch(ps -> ps.getEmployeeId().equals(emp.getId())))
                .collect(Collectors.toList());
        logger.debug("Available employees count: {}", employees.size());

        long leaderCount = employees.stream()
                .filter(emp -> emp.getSkillLevel() != null && emp.getSkillLevel() == PartTimeEmployee.SkillLevel.LEADER)
                .count();
        long generalCount = employees.stream()
                .filter(emp -> emp.getSkillLevel() != null && emp.getSkillLevel() == PartTimeEmployee.SkillLevel.GENERAL)
                .count();
        long newcomerCount = employees.stream()
                .filter(emp -> emp.getSkillLevel() != null && emp.getSkillLevel() == PartTimeEmployee.SkillLevel.NEWCOMER)
                .count();

        Set<Long> amEmployees = preferredShifts.stream()
                .filter(ps -> ps.getTimeSlot() == PreferredShift.TimeSlot.AM)
                .map(PreferredShift::getEmployeeId)
                .collect(Collectors.toSet());
        Set<Long> pmEmployees = preferredShifts.stream()
                .filter(ps -> ps.getTimeSlot() == PreferredShift.TimeSlot.PM)
                .map(PreferredShift::getEmployeeId)
                .collect(Collectors.toSet());
        long totalCountAM = employees.stream().filter(emp -> amEmployees.contains(emp.getId())).count();
        long totalCountPM = employees.stream().filter(emp -> pmEmployees.contains(emp.getId())).count();

        logger.info("AM count: {}, PM count: {}, Leaders: {}, General: {}, Newcomers: {}", 
                    totalCountAM, totalCountPM, leaderCount, generalCount, newcomerCount);

        return Map.of(
                "employees", employees,
                "leaderCount", leaderCount,
                "generalCount", generalCount,
                "newcomerCount", newcomerCount,
                "totalCountAM", totalCountAM,
                "totalCountPM", totalCountPM,
                "preferredShifts", preferredShifts
        );
    }

    public List<ShiftAssignment> assignShifts(
    	    LocalDate workDate,
    	    Map<Long, Map<PreferredShift.TimeSlot, Integer>> workplaceAssignments,
    	    Map<Long, List<Long>> workplaceTasks
    	) {
    	    logger.info("Assigning shifts for workDate: {}", workDate);
    	    logger.debug("Workplace assignments: {}", workplaceAssignments);
    	    logger.debug("Workplace tasks: {}", workplaceTasks);

    	    String dayOfWeek = workDate.getDayOfWeek().toString();
    	    PreferredShift.DayOfWeek targetDay;
    	    try {
    	        targetDay = PreferredShift.DayOfWeek.valueOf(dayOfWeek);
    	    } catch (IllegalArgumentException e) {
    	        logger.error("Invalid day of week: {}", dayOfWeek, e);
    	        throw new IllegalArgumentException("無効な曜日です: " + dayOfWeek);
    	    }

    	    List<PreferredShift> preferredShifts = preferredShiftRepository.findAll().stream()
    	            .filter(ps -> ps.getDayOfWeek() == targetDay)
    	            .toList();
    	    if (preferredShifts.isEmpty()) {
    	        logger.error("No preferred shifts found for day: {}", targetDay);
    	        throw new IllegalStateException("指定された曜日（" + targetDay + "）の希望勤務データがありません");
    	    }

    	    Set<Long> amAvailable = preferredShifts.stream()
    	            .filter(ps -> ps.getTimeSlot() == PreferredShift.TimeSlot.AM)
    	            .map(PreferredShift::getEmployeeId)
    	            .collect(Collectors.toSet());
    	    Set<Long> pmAvailable = preferredShifts.stream()
    	            .filter(ps -> ps.getTimeSlot() == PreferredShift.TimeSlot.PM)
    	            .map(PreferredShift::getEmployeeId)
    	            .collect(Collectors.toSet());

    	    int totalRequiredAM = workplaceAssignments.values().stream()
    	            .mapToInt(map -> map.getOrDefault(PreferredShift.TimeSlot.AM, 0))
    	            .sum();
    	    int totalRequiredPM = workplaceAssignments.values().stream()
    	            .mapToInt(map -> map.getOrDefault(PreferredShift.TimeSlot.PM, 0))
    	            .sum();

    	    logger.info("Required AM: {}, Available AM: {}, Required PM: {}, Available PM: {}",
    	                totalRequiredAM, amAvailable.size(), totalRequiredPM, pmAvailable.size());

    	    if (totalRequiredAM > amAvailable.size()) {
    	        logger.error("AM assignment exceeds available: {} > {}", totalRequiredAM, amAvailable.size());
    	        throw new IllegalStateException("AMの配置人数（" + totalRequiredAM + "人）がAMの出勤予定人数（" + amAvailable.size() + "人）を超えています");
    	    }
    	    if (totalRequiredPM > pmAvailable.size()) {
    	        logger.error("PM assignment exceeds available: {} > {}", totalRequiredPM, pmAvailable.size());
    	        throw new IllegalStateException("PMの配置人数（" + totalRequiredPM + "人）がPMの出勤予定人数（" + pmAvailable.size() + "人）を超えています");
    	    }

    	    List<PartTimeEmployee> availableEmployees = employeeRepository.findAll().stream()
    	            .filter(emp -> preferredShifts.stream().anyMatch(ps -> ps.getEmployeeId().equals(emp.getId())))
    	            .toList();
    	    if (availableEmployees.isEmpty()) {
    	        logger.error("No available employees found for day: {}", targetDay);
    	        throw new IllegalStateException("出勤可能な社員が見つかりません（" + targetDay + "）");
    	    }

    	    List<ShiftAssignment> assignments = new ArrayList<>();
    	    Map<Long, Integer> employeeShiftCount = new HashMap<>();
    	    Map<Long, String> workplaceNames = workplaceRepository.findAll().stream()
    	            .collect(Collectors.toMap(Workplace::getId, Workplace::getName, (v1, v2) -> v1));

    	    try {
    	        for (Long workplaceId : workplaceAssignments.keySet()) {
    	            Map<PreferredShift.TimeSlot, Integer> timeSlotCounts = workplaceAssignments.get(workplaceId);
    	            List<Long> taskIds = workplaceTasks.getOrDefault(workplaceId, List.of());
    	            logger.debug("Processing workplace {} ({}) with tasks: {}", workplaceId, workplaceNames.getOrDefault(workplaceId, "Unknown"), taskIds);
    	            for (PreferredShift.TimeSlot timeSlot : timeSlotCounts.keySet()) {
    	                int requiredCount = timeSlotCounts.get(timeSlot);
    	                if (requiredCount == 0) continue;

    	                List<PartTimeEmployee> leaders = availableEmployees.stream()
    	                        .filter(emp -> emp.getSkillLevel() != null && emp.getSkillLevel() == PartTimeEmployee.SkillLevel.LEADER)
    	                        .filter(emp -> preferredShifts.stream().anyMatch(ps -> ps.getEmployeeId().equals(emp.getId()) && ps.getTimeSlot() == timeSlot))
    	                        .sorted((e1, e2) -> employeeShiftCount.getOrDefault(e1.getId(), 0) - employeeShiftCount.getOrDefault(e2.getId(), 0))
    	                        .toList();

    	                List<PartTimeEmployee> assignedEmployees = new ArrayList<>();
    	                if (!leaders.isEmpty()) {
    	                    assignedEmployees.add(leaders.get(0));
    	                    employeeShiftCount.put(leaders.get(0).getId(), employeeShiftCount.getOrDefault(leaders.get(0).getId(), 0) + 1);
    	                } else {
    	                    logger.error("No leader available for workplace {} ({}) at {}", workplaceId, workplaceNames.getOrDefault(workplaceId, "Unknown"), timeSlot);
    	                    throw new IllegalStateException("作業場 " + workplaceNames.getOrDefault(workplaceId, "ID " + workplaceId) + " の " + timeSlot + " にリーダーが不足しています");
    	                }

    	                int remainingSlots = requiredCount - assignedEmployees.size();
    	                List<PartTimeEmployee> others = availableEmployees.stream()
    	                        .filter(emp -> emp.getSkillLevel() != null && emp.getSkillLevel() != PartTimeEmployee.SkillLevel.LEADER)
    	                        .filter(emp -> preferredShifts.stream().anyMatch(ps -> ps.getEmployeeId().equals(emp.getId()) && ps.getTimeSlot() == timeSlot))
    	                        .sorted((e1, e2) -> employeeShiftCount.getOrDefault(e1.getId(), 0) - employeeShiftCount.getOrDefault(e2.getId(), 0))
    	                        .toList();

    	                if (others.size() < remainingSlots) {
    	                    logger.error("Not enough non-leader employees for workplace {} ({}) at {}: required {}, available {}",
    	                                workplaceId, workplaceNames.getOrDefault(workplaceId, "Unknown"), timeSlot, remainingSlots, others.size());
    	                    throw new IllegalStateException("作業場 " + workplaceNames.getOrDefault(workplaceId, "ID " + workplaceId) + " の " + timeSlot + " に必要な社員数が不足しています（必要：" + remainingSlots + "人、利用可能：" + others.size() + "人）");
    	                }

    	                for (int i = 0; i < remainingSlots; i++) {
    	                    assignedEmployees.add(others.get(i));
    	                    employeeShiftCount.put(others.get(i).getId(), employeeShiftCount.getOrDefault(others.get(i).getId(), 0) + 1);
    	                }

    	                Optional<Workplace> workplaceOpt = workplaceRepository.findById(workplaceId);
    	                if (!workplaceOpt.isPresent()) {
    	                    logger.error("Workplace not found: {}", workplaceId);
    	                    throw new IllegalStateException("作業場ID " + workplaceId + "が見つかりません");
    	                }
    	                Workplace workplace = workplaceOpt.get();

    	                Optional<Task> defaultTaskOpt = taskRepository.findByName("未指定");
    	                if (!defaultTaskOpt.isPresent() && workplaceId <= 4L && taskIds.isEmpty()) {
    	                    logger.error("Default task '未指定' not found");
    	                    throw new IllegalStateException("タスク '未指定' が見つかりません。データベースに登録してください");
    	                }
    	                Task defaultTask = defaultTaskOpt.orElse(null);

    	                for (PartTimeEmployee emp : assignedEmployees) {
    	                    if (workplaceId.equals(5L)) {
    	                        Optional<Task> selectionTaskOpt = taskRepository.findByName("選果");
    	                        if (!selectionTaskOpt.isPresent()) {
    	                            logger.error("Selection task '選果' not found");
    	                            throw new IllegalStateException("タスク '選果' が見つかりません。データベースに登録してください");
    	                        }
    	                        ShiftAssignment assignment = new ShiftAssignment();
    	                        assignment.setWorkDate(workDate);
    	                        assignment.setWorkplace(workplace);
    	                        assignment.setTimeSlot(timeSlot);
    	                        assignment.setEmployee(emp);
    	                        assignment.setRequiredCount(requiredCount);
    	                        assignment.setTask(selectionTaskOpt.get());
    	                        assignments.add(assignment);
    	                        logger.debug("Added assignment: workplaceId={}, timeSlot={}, employeeId={}, taskId={}",
    	                                     workplaceId, timeSlot, emp.getId(), selectionTaskOpt.get().getId());
    	                    } else if (taskIds.isEmpty()) {
    	                        ShiftAssignment assignment = new ShiftAssignment();
    	                        assignment.setWorkDate(workDate);
    	                        assignment.setWorkplace(workplace);
    	                        assignment.setTimeSlot(timeSlot);
    	                        assignment.setEmployee(emp);
    	                        assignment.setRequiredCount(requiredCount);
    	                        assignment.setTask(defaultTask);
    	                        assignments.add(assignment);
    	                        logger.debug("Added assignment: workplaceId={}, timeSlot={}, employeeId={}, taskId={}",
    	                                     workplaceId, timeSlot, emp.getId(), defaultTask != null ? defaultTask.getId() : null);
    	                    } else {
    	                        for (Long taskId : taskIds) {
    	                            Optional<Task> taskOpt = taskRepository.findById(taskId);
    	                            if (!taskOpt.isPresent()) {
    	                                logger.error("Task not found: {}", taskId);
    	                                throw new IllegalStateException("タスクID " + taskId + "が見つかりません");
    	                            }
    	                            ShiftAssignment taskAssignment = new ShiftAssignment();
    	                            taskAssignment.setWorkDate(workDate);
    	                            taskAssignment.setWorkplace(workplace);
    	                            taskAssignment.setTimeSlot(timeSlot);
    	                            taskAssignment.setEmployee(emp);
    	                            taskAssignment.setRequiredCount(requiredCount);
    	                            taskAssignment.setTask(taskOpt.get());
    	                            assignments.add(taskAssignment);
    	                            logger.debug("Added assignment: workplaceId={}, timeSlot={}, employeeId={}, taskId={}",
    	                                         workplaceId, timeSlot, emp.getId(), taskId);
    	                        }
    	                    }
    	                }
    	            }
    	        }

    	        logger.info("Generated {} shift assignments", assignments.size());
    	        logger.debug("Assignments details: {}", assignments);
    	        shiftAssignmentRepository.saveAll(assignments);
    	        logger.info("Successfully saved {} shift assignments", assignments.size());
    	        return assignments;

    	    } catch (Exception e) {
    	        logger.error("Unexpected error during shift assignment: {}", e.getMessage(), e);
    	        throw new RuntimeException("シフト割り当て中に予期しないエラーが発生しました: " + e.getMessage(), e);
    	    }
    	}
}
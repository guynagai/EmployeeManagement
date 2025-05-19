package com.example.app.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.app.entity.PartTimeEmployee;
import com.example.app.entity.PreferredShift;
import com.example.app.repository.PartTimeEmployeeRepository;
import com.example.app.repository.PreferredShiftRepository;

@Service
@SuppressWarnings({"unchecked", "unused"})
public class ShiftService {

    private static final Logger logger = LoggerFactory.getLogger(ShiftService.class);

    @Autowired
    private PartTimeEmployeeRepository employeeRepository;

    @Autowired
    private PreferredShiftRepository preferredShiftRepository;

    public Map<String, Object> getAvailableEmployees(String workDate) {
        logger.info("Fetching available employees for workDate: {}", workDate);
        LocalDate date;
        try {
            date = LocalDate.parse(workDate);
        } catch (Exception e) {
            logger.error("Invalid date format: {}", workDate, e);
            return Map.of("error", "無効な日付形式です: " + workDate);
        }

        String dayOfWeek = date.getDayOfWeek().toString();
        PreferredShift.DayOfWeek targetDay;
        try {
            targetDay = PreferredShift.DayOfWeek.valueOf(dayOfWeek);
        } catch (IllegalArgumentException e) {
            logger.error("Invalid day of week: {}", dayOfWeek, e);
            return Map.of("error", "無効な曜日です: " + dayOfWeek);
        }

        List<PreferredShift> preferredShifts = preferredShiftRepository.findByDayOfWeek(targetDay);
        logger.debug("Preferred shifts count: {}", preferredShifts.size());

        List<PartTimeEmployee> employees = employeeRepository.findAll().stream()
                .filter(emp -> emp != null && emp.getId() != null)
                .filter(emp -> preferredShifts.stream().anyMatch(ps -> ps.getEmployeeId() != null && ps.getEmployeeId().equals(emp.getId())))
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
                .filter(ps -> ps.getTimeSlot() != null && ps.getTimeSlot() == PreferredShift.TimeSlot.AM)
                .map(PreferredShift::getEmployeeId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        Set<Long> pmEmployees = preferredShifts.stream()
                .filter(ps -> ps.getTimeSlot() != null && ps.getTimeSlot() == PreferredShift.TimeSlot.PM)
                .map(PreferredShift::getEmployeeId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        long totalCountAM = employees.stream().filter(emp -> amEmployees.contains(emp.getId())).count();
        long totalCountPM = employees.stream().filter(emp -> pmEmployees.contains(emp.getId())).count();

        logger.info("AM count: {}, PM count: {}, Leaders: {}, General: {}, Newcomers: {}", 
                    totalCountAM, totalCountPM, leaderCount, generalCount, newcomerCount);

        Map<String, Object> result = new HashMap<>();
        result.put("employees", employees != null ? employees : new ArrayList<>());
        result.put("preferredShifts", preferredShifts != null ? preferredShifts : new ArrayList<>());
        result.put("leaderCount", leaderCount);
        result.put("generalCount", generalCount);
        result.put("newcomerCount", newcomerCount);
        result.put("totalCountAM", totalCountAM);
        result.put("totalCountPM", totalCountPM);
        return result;
    }

    /*
    // コメントアウト：リスト表示に不要、警告削減
    public List<ShiftAssignment> assignShifts(
            LocalDate workDate,
            Map<Long, Map<PreferredShift.TimeSlot, Integer>> workplaceAssignments,
            Map<Long, List<Long>> workplaceTasks) {
        // 実装は省略（必要なら後で復元）
    }
    */
}
package com.example.app.controller;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin/shifts")
public class ShiftController {
    private static final Logger logger = LoggerFactory.getLogger(ShiftController.class);

    @GetMapping("/assign")
    public String showAssignmentForm(
            @RequestParam(value = "workDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate workDate,
            Model model) {
        logger.info("Showing shift assignment form for workDate: {}", workDate);
        model.addAttribute("workDate", workDate != null ? workDate : LocalDate.now());
        model.addAttribute("workplaces", Arrays.asList("ハウス1", "ハウス2", "ハウス3", "ハウス4", "選果場"));
        model.addAttribute("tasks", Arrays.asList("収穫", "梱包", "選果"));
        return "employees/shift_assignment_form";
    }

    @PostMapping("/assign")
    public String assignShifts(
            @RequestParam("workDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate workDate,
            @RequestParam Map<String, String> allParams,
            Model model) {
        logger.info("Processing shift assignment for workDate: {}", workDate);
        logger.debug("Raw form parameters: {}", allParams);

        Map<Long, Map<String, Map<String, Object>>> workplaceAssignments = new HashMap<>();
        for (Map.Entry<String, String> entry : allParams.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            logger.debug("Processing key: {}, value: {}", key, value);

            if (key.startsWith("assignments[")) {
                Matcher matcher = Pattern.compile("assignments\\[(\\d+)\\]\\[(\\w+)\\]\\[(\\w+)\\]").matcher(key);
                logger.debug("Regex matches: {}", matcher.matches());
                if (matcher.matches()) {
                    Long workplaceId = Long.parseLong(matcher.group(1));
                    String timeSlotStr = matcher.group(2);
                    String type = matcher.group(3);
                    logger.debug("Parsed: workplaceId={}, timeSlotStr={}, type={}", workplaceId, timeSlotStr, type);

                    Map<String, Object> slotData = workplaceAssignments
                        .computeIfAbsent(workplaceId, k -> new HashMap<>())
                        .computeIfAbsent(timeSlotStr, k -> new HashMap<>());

                    if ("count".equals(type)) {
                        try {
                            int count = value.isEmpty() ? 0 : Integer.parseInt(value);
                            slotData.put("count", count);
                        } catch (NumberFormatException e) {
                            logger.warn("Invalid count value: {}", value);
                        }
                    } else if ("task".equals(type)) {
                        slotData.put("task", value);
                    }
                    logger.debug("Updated workplaceAssignments: {}", workplaceAssignments);
                } else {
                    logger.warn("Key does not match regex: {}", key);
                }
            }
        }

        logger.debug("Parsed workplaceAssignments: {}", workplaceAssignments);
        model.addAttribute("workDate", workDate);
        model.addAttribute("formData", allParams);
        model.addAttribute("assignments", workplaceAssignments);
        return "employees/shift_assignment_result";
    }
}
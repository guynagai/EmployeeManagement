package com.example.app.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.app.entity.PreferredShift;

@Repository
public interface PreferredShiftRepository extends JpaRepository<PreferredShift, Long> {
    List<PreferredShift> findByEmployeeId(Long employeeId);
    List<PreferredShift> findByDayOfWeek(PreferredShift.DayOfWeek dayOfWeek);
}
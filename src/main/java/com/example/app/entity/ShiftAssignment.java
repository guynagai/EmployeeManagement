package com.example.app.entity;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "shift_assignments")
public class ShiftAssignment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "work_date")
    private LocalDate workDate;

    @ManyToOne
    @JoinColumn(name = "workplace_id")
    private Workplace workplace;

    @ManyToOne
    @JoinColumn(name = "task_id")
    private Task task;

    @Enumerated(EnumType.STRING)
    @Column(name = "time_slot")
    private PreferredShift.TimeSlot timeSlot;

    @Column(name = "required_count")
    private Integer requiredCount;

    @ManyToOne
    @JoinColumn(name = "employee_id")
    private PartTimeEmployee employee;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDate getWorkDate() { return workDate; }
    public void setWorkDate(LocalDate workDate) { this.workDate = workDate; }

    public Workplace getWorkplace() { return workplace; }
    public void setWorkplace(Workplace workplace) { this.workplace = workplace; }

    public Task getTask() { return task; }
    public void setTask(Task task) { this.task = task; }

    public PreferredShift.TimeSlot getTimeSlot() { return timeSlot; }
    public void setTimeSlot(PreferredShift.TimeSlot timeSlot) { this.timeSlot = timeSlot; }

    public Integer getRequiredCount() { return requiredCount; }
    public void setRequiredCount(Integer requiredCount) { this.requiredCount = requiredCount; }

    public PartTimeEmployee getEmployee() { return employee; }
    public void setEmployee(PartTimeEmployee employee) { this.employee = employee; }
}
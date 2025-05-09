package com.example.app.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.app.entity.PartTimeEmployee;

public interface PartTimeEmployeeRepository extends JpaRepository<PartTimeEmployee, Integer> {
}

package com.example.app.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.app.entity.Workplace;

@Repository
public interface WorkplaceRepository extends JpaRepository<Workplace, Long> {
}
package com.example.app.service;

     import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.example.app.entity.PartTimeEmployee;

     public interface PartTimeEmployeeService {
         Page<PartTimeEmployee> findAll(Pageable pageable);
         Optional<PartTimeEmployee> findById(Integer id);
         PartTimeEmployee save(PartTimeEmployee employee);
     }
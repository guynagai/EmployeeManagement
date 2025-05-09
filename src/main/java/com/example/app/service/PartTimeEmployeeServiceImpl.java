package com.example.app.service;

    import java.util.List;

import org.springframework.stereotype.Service;

import com.example.app.entity.PartTimeEmployee;
import com.example.app.repository.PartTimeEmployeeRepository;

    @Service
    public class PartTimeEmployeeServiceImpl implements PartTimeEmployeeService {

        private final PartTimeEmployeeRepository repository;

        public PartTimeEmployeeServiceImpl(PartTimeEmployeeRepository repository) {
            this.repository = repository;
        }

        @Override
        public List<PartTimeEmployee> findAll() {
            List<PartTimeEmployee> employees = repository.findAll();
            System.out.println("Service findAll: " + employees); // デバッグ用
            return employees;
        }
    }
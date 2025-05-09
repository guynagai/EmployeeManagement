package com.example.app.service;

     import java.util.List;
import java.util.Optional;

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
             return repository.findAll();
         }

         @Override
         public Optional<PartTimeEmployee> findById(Integer id) {
             return repository.findById(id);
         }

         @Override
         public PartTimeEmployee save(PartTimeEmployee employee) {
             return repository.save(employee);
         }
     }
package com.example.app.service;

     import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
         public Page<PartTimeEmployee> findAll(Pageable pageable) {
             return repository.findAll(pageable);
         }

         @Override
         public Optional<PartTimeEmployee> findById(Integer id) {
             return repository.findById(id);
         }

         @Override
         public PartTimeEmployee save(PartTimeEmployee employee) {
             return repository.save(employee);
         }
         
         @Override
         public void deleteById(Integer id) {
             repository.deleteById(id);
         }
         
     }
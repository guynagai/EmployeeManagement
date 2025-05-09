package com.example.app.service;

     import java.util.List;
import java.util.Optional;

import com.example.app.entity.PartTimeEmployee;

     public interface PartTimeEmployeeService {
         List<PartTimeEmployee> findAll();
         Optional<PartTimeEmployee> findById(Integer id);
         PartTimeEmployee save(PartTimeEmployee employee);
     }


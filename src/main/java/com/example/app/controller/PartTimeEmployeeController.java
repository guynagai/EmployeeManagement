package com.example.app.controller;

  import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.app.entity.PartTimeEmployee;
import com.example.app.service.PartTimeEmployeeService;

  @Controller
  @RequestMapping("/admin/employees")
  public class PartTimeEmployeeController {
      private final PartTimeEmployeeService service;

      public PartTimeEmployeeController(PartTimeEmployeeService service) {
          this.service = service;
      }

      @GetMapping({"", "/"})
      public String list(Model model) {
          model.addAttribute("employees", service.findAll());
          return "employees/list";
      }

      @GetMapping("/edit/{id}")
      public String edit(@PathVariable Integer id, Model model) {
          PartTimeEmployee employee = service.findById(id)
              .orElseThrow(() -> new IllegalArgumentException("Invalid employee ID"));
          model.addAttribute("employee", employee);
          return "employees/edit";
      }

      @PostMapping("/edit/{id}")
      public String update(@PathVariable Integer id, PartTimeEmployee employee) {
          employee.setId(id);
          service.save(employee);
          return "redirect:/admin/employees";
      }
      
      @GetMapping("/add")
      public String addForm(Model model) {
          model.addAttribute("employee", new PartTimeEmployee());
          return "employees/add";
      }

      @PostMapping("/add")
      public String add(PartTimeEmployee employee) {
          service.save(employee);
          return "redirect:/admin/employees";
      }   
      
  }


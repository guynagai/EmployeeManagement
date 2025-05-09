package com.example.app.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
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
        List<PartTimeEmployee> employees = service.findAll();
        System.out.println("Controller Employees: " + employees); // デバッグ用
        model.addAttribute("employees", employees);
        return "employees/list";
    }
}


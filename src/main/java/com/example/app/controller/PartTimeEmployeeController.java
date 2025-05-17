package com.example.app.controller;

import java.time.LocalDate;
import java.time.Period;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
    public String list(@RequestParam(defaultValue = "0") int page, Model model) {
        Pageable pageable = PageRequest.of(page, 10);
        Page<PartTimeEmployee> employeePage = service.findAll(pageable);
        model.addAttribute("employees", employeePage.getContent());
        model.addAttribute("page", employeePage);
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
    public String update(@PathVariable Long id, PartTimeEmployee employee) {
        employee.setId(id);
        if (employee.getBirthdate() != null) {
            employee.setAge(Period.between(employee.getBirthdate(), LocalDate.now()).getYears());
        }
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
        if (employee.getBirthdate() != null) {
            employee.setAge(Period.between(employee.getBirthdate(), LocalDate.now()).getYears());
        }
        service.save(employee);
        return "redirect:/admin/employees";
    }
    
    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            service.deleteById(id);
            redirectAttributes.addFlashAttribute("message", "従業員を削除しました。");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "従業員の削除に失敗しました。関連するシフト情報が存在する可能性があります。");
        }
        return "redirect:/admin/employees";
    }
}


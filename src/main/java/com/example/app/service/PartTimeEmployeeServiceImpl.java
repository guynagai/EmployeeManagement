package com.example.app.service;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.app.entity.PartTimeEmployee;
import com.example.app.repository.PartTimeEmployeeRepository;

@Service
public class PartTimeEmployeeServiceImpl implements PartTimeEmployeeService {
    private final PartTimeEmployeeRepository repository;
    private final PreferredShiftService preferredShiftService;

    public PartTimeEmployeeServiceImpl(PartTimeEmployeeRepository repository, PreferredShiftService preferredShiftService) {
        this.repository = repository;
        this.preferredShiftService = preferredShiftService;
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
    @Transactional
    public void deleteById(Integer id) {
        preferredShiftService.deleteByEmployeeId(id.longValue());
        repository.deleteById(id);
    }
}
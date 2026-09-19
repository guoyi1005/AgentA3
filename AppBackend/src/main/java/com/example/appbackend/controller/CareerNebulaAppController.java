package com.example.appbackend.controller;

import com.example.appbackend.entity.Result;
import com.example.appbackend.service.CareerNebulaService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Student-facing, read-only access to the configured career map. */
@RestController
@RequestMapping("/api/app/career-nebula")
public class CareerNebulaAppController {

    private final CareerNebulaService service;

    public CareerNebulaAppController(CareerNebulaService service) {
        this.service = service;
    }

    @GetMapping
    public Result<Map<String, Object>> getMap() {
        return Result.success(service.getMap());
    }
}

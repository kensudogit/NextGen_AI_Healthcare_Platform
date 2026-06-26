package com.nghealth.platform.web;

import com.nghealth.platform.service.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/stats")
    public Map<String, Object> stats() {
        return dashboardService.stats();
    }

    @GetMapping("/appointments")
    public Map<String, Object> appointments() {
        return Map.of("appointments", dashboardService.listAppointments());
    }
}

package com.erwin.backend.controller;

import com.erwin.backend.dtos.DashboardResumenDto;
import com.erwin.backend.dtos.DashboardDetalleDto;
import com.erwin.backend.service.DashboardService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "http://localhost:4200")
public class DashboardController {
    private final DashboardService service;

    public DashboardController(DashboardService service) {
        this.service = service;
    }

    @GetMapping("/resumen")
    public DashboardResumenDto resumen() {
        return service.obtenerResumen();
    }

    @GetMapping("/detalle")
    public DashboardDetalleDto detalle() {
        return service.obtenerDetalle();
    }
}

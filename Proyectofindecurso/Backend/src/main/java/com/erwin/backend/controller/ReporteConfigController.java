package com.erwin.backend.controller;
import com.erwin.backend.entities.ReporteConfig;
import com.erwin.backend.service.ReporteConfigService;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reporte-config")
@CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
public class ReporteConfigController {

    private final ReporteConfigService svc;
    public ReporteConfigController(ReporteConfigService svc) { this.svc = svc; }

    @GetMapping
    public List<ReporteConfig> getAll() { return svc.getAll(); }

    @PutMapping("/{id}")
    public ReporteConfig update(@PathVariable Integer id, @RequestBody Map<String, String> body) {
        return svc.update(id, body.get("valor"));
    }
}

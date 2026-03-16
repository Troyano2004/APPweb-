package com.erwin.backend.service;
import com.erwin.backend.entities.ReporteConfig;
import com.erwin.backend.repository.ReporteConfigRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ReporteConfigService {

    private final ReporteConfigRepository repo;
    public ReporteConfigService(ReporteConfigRepository repo) { this.repo = repo; }

    public List<ReporteConfig> getAll() { return repo.findAll(); }

    public ReporteConfig update(Integer id, String valor) {
        ReporteConfig c = repo.findById(id)
            .orElseThrow(() -> new RuntimeException("Config no encontrada"));
        c.setValor(valor);
        return repo.save(c);
    }

    public String get(String clave) {
        return repo.findByClave(clave).map(ReporteConfig::getValor).orElse(null);
    }

    public boolean isEnabled(String clave) {
        return "true".equalsIgnoreCase(get(clave));
    }
}

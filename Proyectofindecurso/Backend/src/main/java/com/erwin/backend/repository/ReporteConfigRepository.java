package com.erwin.backend.repository;
import com.erwin.backend.entities.ReporteConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ReporteConfigRepository extends JpaRepository<ReporteConfig, Integer> {
    Optional<ReporteConfig> findByClave(String clave);
}

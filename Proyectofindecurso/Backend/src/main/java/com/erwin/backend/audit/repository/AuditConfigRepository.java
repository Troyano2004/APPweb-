package com.erwin.backend.audit.repository;
import com.erwin.backend.audit.entity.AuditConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.*;

@Repository
public interface AuditConfigRepository extends JpaRepository<AuditConfig, Integer> {
    Optional<AuditConfig> findByEntidadAndAccion(String entidad, String accion);
    List<AuditConfig> findByActivoTrue();
    boolean existsByEntidadAndAccion(String entidad, String accion);
}

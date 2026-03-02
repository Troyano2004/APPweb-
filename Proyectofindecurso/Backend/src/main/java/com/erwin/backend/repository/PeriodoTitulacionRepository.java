package com.erwin.backend.repository;

import com.erwin.backend.entities.PeriodoTitulacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PeriodoTitulacionRepository extends JpaRepository<PeriodoTitulacion, Integer> {
    Optional<PeriodoTitulacion> findByActivoTrue();
    List<PeriodoTitulacion> findByActivo(Boolean activo);
    Optional<PeriodoTitulacion> findFirstByActivoTrueOrderByIdPeriodoDesc();
}

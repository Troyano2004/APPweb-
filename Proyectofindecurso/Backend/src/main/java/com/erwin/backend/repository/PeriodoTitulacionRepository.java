package com.erwin.backend.repository;

import com.erwin.backend.entities.PeriodoTitulacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PeriodoTitulacionRepository extends JpaRepository<PeriodoTitulacion, Integer> {
    Optional<PeriodoTitulacion> findByActivoTrue();
}

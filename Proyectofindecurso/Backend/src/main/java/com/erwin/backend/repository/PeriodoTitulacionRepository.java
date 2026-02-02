package com.erwin.backend.repository;

import com.erwin.backend.entities.Periodotitulacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PeriodoTitulacionRepository extends JpaRepository<Periodotitulacion, Integer> {
    Optional<Periodotitulacion> findByActivoTrue();
}

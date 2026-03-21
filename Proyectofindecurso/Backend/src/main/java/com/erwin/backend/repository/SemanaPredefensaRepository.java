package com.erwin.backend.repository;

import com.erwin.backend.entities.SemanaPredefensa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SemanaPredefensaRepository extends JpaRepository<SemanaPredefensa, Integer> {

    // Obtener la semana activa más reciente
    Optional<SemanaPredefensa> findTopByActivoTrueOrderByIdSemanaDesc();

    // Obtener por período
    Optional<SemanaPredefensa> findTopByPeriodo_IdPeriodoAndActivoTrueOrderByIdSemanaDesc(Integer idPeriodo);
}
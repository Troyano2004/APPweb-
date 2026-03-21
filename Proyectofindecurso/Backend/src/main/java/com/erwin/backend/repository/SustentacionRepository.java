package com.erwin.backend.repository;

import com.erwin.backend.entities.Sustentacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

import java.util.List;
@Repository
public interface SustentacionRepository extends JpaRepository<Sustentacion, Integer> {
    List<Sustentacion> findByProyecto_IdProyectoOrderByFechaDescHoraDesc(Integer idProyecto);
    List<Sustentacion> findByTipoAndFechaBetween(String tipo, LocalDate desde, LocalDate hasta);
}
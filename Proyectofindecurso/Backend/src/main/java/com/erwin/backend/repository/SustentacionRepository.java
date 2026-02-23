package com.erwin.backend.repository;

import com.erwin.backend.entities.Sustentacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SustentacionRepository extends JpaRepository<Sustentacion, Integer> {
    List<Sustentacion> findByProyecto_IdProyectoOrderByFechaDescHoraDesc(Integer idProyecto);
}
package com.erwin.backend.repository;

import com.erwin.backend.entities.TribunalProyecto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TribunalProyectoRepository extends JpaRepository<TribunalProyecto, Integer> {
    List<TribunalProyecto> findByProyecto_IdProyecto(Integer idProyecto);
    long countByProyecto_IdProyecto(Integer idProyecto);
    void deleteByProyecto_IdProyecto(Integer idProyecto);
}
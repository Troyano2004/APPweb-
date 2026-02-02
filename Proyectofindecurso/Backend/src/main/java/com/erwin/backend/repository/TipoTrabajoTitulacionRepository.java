package com.erwin.backend.repository;

import com.erwin.backend.entities.Tipotrabajotitulacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TipoTrabajoTitulacionRepository extends JpaRepository<Tipotrabajotitulacion,Integer> {
    List<Tipotrabajotitulacion> findByModalidadTitulacion_IdModalidad(Integer modalidadTitulacionIdModalidad);
}

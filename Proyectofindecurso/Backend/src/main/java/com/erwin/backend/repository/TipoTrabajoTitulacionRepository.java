package com.erwin.backend.repository;

import com.erwin.backend.entities.Tipotrabajotitulacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TipoTrabajoTitulacionRepository extends JpaRepository<Tipotrabajotitulacion,Integer> {
    List<Tipotrabajotitulacion> findByModalidadTitulacion_IdModalidad(Integer modalidadTitulacionIdModalidad);
    List<Tipotrabajotitulacion> findByModalidadTitulacionIdModalidad(Integer idModalidad);
}

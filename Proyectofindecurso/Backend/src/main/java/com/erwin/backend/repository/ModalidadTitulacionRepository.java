package com.erwin.backend.repository;

import com.erwin.backend.entities.Modalidadtitulacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ModalidadTitulacionRepository extends JpaRepository<Modalidadtitulacion, Integer> {
    Optional<Modalidadtitulacion> findByNombreIgnoreCase(String nombre);
}

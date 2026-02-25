package com.erwin.backend.repository;

import com.erwin.backend.entities.Facultad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FacultadRepository extends JpaRepository<Facultad, Integer> {
    List<Facultad> findByUniversidadIdUniversidad(Integer idUniversidad);
}

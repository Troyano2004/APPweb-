package com.erwin.backend.repository;

import com.erwin.backend.entities.Coordinador;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CoordinadorRepository extends JpaRepository<Coordinador, Integer> {
    Optional<Coordinador> findByUsuario_IdUsuarioAndActivoTrue(Integer idUsuario);
    List<Coordinador> findByActivoTrue();
    Optional<Coordinador> findByCarrera_IdCarreraAndActivoTrue(Integer idCarrera);
    boolean existsByCarrera_IdCarreraAndActivoTrue(Integer idCarrera);

}
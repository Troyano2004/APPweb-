package com.erwin.backend.repository;

import com.erwin.backend.dtos.SesionActivaDto;
import com.erwin.backend.entities.SesionActiva;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SesionActivaRepository extends JpaRepository<SesionActiva, Integer> {
    List<SesionActiva> findByActivoTrue();
    Optional<SesionActiva> findBySessionId(String sessionId);
    void deleteByActivoFalseAndUltimaActividadBefore(LocalDateTime fecha);
    List<SesionActiva> findByIdUsuarioAndActivoTrue(Integer idUsuario);

}

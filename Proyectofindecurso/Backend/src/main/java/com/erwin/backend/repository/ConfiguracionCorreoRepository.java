package com.erwin.backend.repository;

import com.erwin.backend.entities.ConfiguracionCorreo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConfiguracionCorreoRepository extends JpaRepository<ConfiguracionCorreo, Integer> {
    Optional<ConfiguracionCorreo> findFirstByActivoTrue();
}
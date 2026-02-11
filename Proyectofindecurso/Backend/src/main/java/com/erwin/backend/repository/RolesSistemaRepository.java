package com.erwin.backend.repository;

import com.erwin.backend.entities.RolSistema;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RolesSistemaRepository extends JpaRepository<RolSistema, Integer> {
    Optional<RolSistema> findByNombreRol(String nombreRol);
}
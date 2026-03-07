package com.erwin.backend.repository;

import com.erwin.backend.entities.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

    Optional<Usuario> findByUsername(String username);
    boolean existsByCorreoInstitucionalIgnoreCase(String correo);
    boolean existsByCedula(String cedula);
    boolean existsByUsername(String username);
}
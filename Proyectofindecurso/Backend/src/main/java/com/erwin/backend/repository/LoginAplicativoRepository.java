package com.erwin.backend.repository;

import com.erwin.backend.entities.Loginaplicativo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LoginAplicativoRepository extends JpaRepository<Loginaplicativo, Integer> {

    // ✅ Ignora mayúsculas/minúsculas
    Optional<Loginaplicativo> findByUsuarioLoginIgnoreCase(String usuarioLogin);

    Optional<Loginaplicativo> findByUsuario_IdUsuario(Integer idUsuario);
}
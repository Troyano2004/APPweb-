package com.erwin.backend.repository;

import com.erwin.backend.entities.Loginbd;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LoginBdRepository extends JpaRepository<Loginbd,Integer> {
    Optional<Loginbd> findByUsuario_IdUsuario(Integer usuarioIdUsuario);
}

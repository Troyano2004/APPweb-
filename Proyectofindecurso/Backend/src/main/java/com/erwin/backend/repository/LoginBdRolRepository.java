package com.erwin.backend.repository;

import com.erwin.backend.entities.LoginBdRol;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LoginBdRolRepository extends JpaRepository<LoginBdRol,Integer> {
    List<LoginBdRol> findById_IdLoginBd(Long idIdLoginBd);
}

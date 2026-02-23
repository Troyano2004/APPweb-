package com.erwin.backend.repository;

import com.erwin.backend.entities.AvalDirector;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AvalDirectorRepository extends JpaRepository<AvalDirector, Integer> {
    Optional<AvalDirector> findByDocumento_Id(Integer idDocumento);
}
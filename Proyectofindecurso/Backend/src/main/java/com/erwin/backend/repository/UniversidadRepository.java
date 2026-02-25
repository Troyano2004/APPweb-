package com.erwin.backend.repository;

import com.erwin.backend.entities.Universidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UniversidadRepository extends JpaRepository<Universidad, Integer> {
}

package com.erwin.backend.repository;

import com.erwin.backend.entities.ActaRevisionTutor;
import com.erwin.backend.entities.ActaRevisionTutor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ActaRevisionDirectorRepository extends JpaRepository<ActaRevisionTutor, Integer> {
    Optional<ActaRevisionTutor> findByTutoria_IdTutoria(Integer idTutoria);
    boolean existsByTutoria_IdTutoria(Integer idTutoria);

}
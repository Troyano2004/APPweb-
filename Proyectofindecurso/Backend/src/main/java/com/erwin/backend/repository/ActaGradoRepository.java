package com.erwin.backend.repository;
import com.erwin.backend.entities.ActaGrado;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface ActaGradoRepository extends JpaRepository<ActaGrado, Integer> {
    Optional<ActaGrado> findByEstudiante_IdEstudiante(Integer idEstudiante);
    boolean existsByEstudiante_IdEstudiante(Integer idEstudiante);
}
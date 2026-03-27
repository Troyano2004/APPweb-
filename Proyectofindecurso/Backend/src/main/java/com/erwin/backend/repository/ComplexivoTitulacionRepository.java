package com.erwin.backend.repository;

import com.erwin.backend.entities.ComplexivoTitulacion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ComplexivoTitulacionRepository
        extends JpaRepository<ComplexivoTitulacion, Integer> {

    Optional<ComplexivoTitulacion> findByEstudiante_IdEstudianteAndPeriodo_IdPeriodo(
            Integer idEstudiante, Integer idPeriodo);
}



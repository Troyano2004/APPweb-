package com.erwin.backend.repository;

import com.erwin.backend.entities.ComplexivoInformePractico;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ComplexivoInformePracticoRepository
        extends JpaRepository<ComplexivoInformePractico, Integer> {

    Optional<ComplexivoInformePractico> findByComplexivo_IdComplexivo(Integer idComplexivo);
}
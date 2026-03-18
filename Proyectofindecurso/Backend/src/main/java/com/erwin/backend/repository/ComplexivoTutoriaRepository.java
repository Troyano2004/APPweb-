package com.erwin.backend.repository;

import com.erwin.backend.entities.ComplexivoTutoria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ComplexivoTutoriaRepository extends JpaRepository<ComplexivoTutoria, Integer> {

    List<ComplexivoTutoria> findByInforme_Complexivo_IdComplexivo(Integer idComplexivo);
}
package com.erwin.backend.repository;

import com.erwin.backend.entities.IaEjemplo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IaEjemploRepository extends JpaRepository<IaEjemplo, Integer> {
    List<IaEjemplo> findTop10ByIdEstudiante(Integer idEstudiante);
}

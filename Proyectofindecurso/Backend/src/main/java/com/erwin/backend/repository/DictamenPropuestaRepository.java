package com.erwin.backend.repository;

import com.erwin.backend.entities.DictamenPropuesta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DictamenPropuestaRepository extends JpaRepository<DictamenPropuesta, Integer> {
    List<DictamenPropuesta> findByPropuesta_IdPropuesta(Integer IdPropuesta);
}

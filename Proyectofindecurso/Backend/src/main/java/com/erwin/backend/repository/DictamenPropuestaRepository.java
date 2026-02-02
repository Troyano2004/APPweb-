package com.erwin.backend.repository;

import com.erwin.backend.entities.Dictamenpropuesta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DictamenPropuestaRepository extends JpaRepository<Dictamenpropuesta, Integer> {
    List<Dictamenpropuesta> findByPropuesta_IdPropuesta(Integer IdPropuesta);
}

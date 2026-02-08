package com.erwin.backend.repository;

import com.erwin.backend.entities.ComisionMiembro;
import com.erwin.backend.entities.comisionmiembroid;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ComisionMiembroRepository extends JpaRepository<ComisionMiembro, comisionmiembroid> {
    List<ComisionMiembro> findByIdcomision_IdComision(Integer idComision);
}

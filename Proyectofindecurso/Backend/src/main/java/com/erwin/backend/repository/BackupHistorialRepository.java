package com.erwin.backend.repository;

import com.erwin.backend.entities.BackupHistorial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BackupHistorialRepository extends JpaRepository<BackupHistorial, Integer> {

    // Todos ordenados por fecha descendente
    List<BackupHistorial> findAllByOrderByFechaCreacionDesc();

    // Los más antiguos primero — para poder eliminar los que superen la cantidad máxima
    @Query("SELECT h FROM BackupHistorial h WHERE h.estado = 'EXITOSO' ORDER BY h.fechaCreacion ASC")
    List<BackupHistorial> findExitososMasAntiguosPrimero();

    // Contar exitosos — para controlar el límite de cantidad
    long countByEstado(String estado);
}
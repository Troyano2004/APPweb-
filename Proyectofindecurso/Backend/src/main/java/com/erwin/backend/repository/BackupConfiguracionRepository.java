
package com.erwin.backend.repository;

import com.erwin.backend.entities.BackupConfiguracion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BackupConfiguracionRepository extends JpaRepository<BackupConfiguracion, Integer> {
    // Solo existe una fila de configuración (id=1), así que con findById(1) es suficiente
}
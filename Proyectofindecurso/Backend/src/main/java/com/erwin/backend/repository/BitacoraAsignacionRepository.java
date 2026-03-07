package com.erwin.backend.repository;
import com.erwin.backend.entities.BitacoraAsignacion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface BitacoraAsignacionRepository extends JpaRepository<BitacoraAsignacion, Integer> {
    List<BitacoraAsignacion> findByProyecto_IdProyectoOrderByFechaDesc(Integer idProyecto);
}
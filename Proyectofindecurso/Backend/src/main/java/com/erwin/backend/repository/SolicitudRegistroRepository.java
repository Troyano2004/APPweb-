package com.erwin.backend.repository;

import com.erwin.backend.entities.SolicitudRegistro;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface SolicitudRegistroRepository extends JpaRepository<SolicitudRegistro, Integer> {
    Optional<SolicitudRegistro> findByCorreo(String correo);
    List<SolicitudRegistro> findByEstado(String estado);
    boolean existsByCedula(String cedula);
    boolean existsByCorreo(String correo);
    Optional<SolicitudRegistro> findByCorreoAndEstado(String correo, String estado);
    boolean existsByCorreoAndEstado(String correo, String estado);
    boolean existsByCorreoAndEstadoIn(String correo, List<String> estados);
    List<SolicitudRegistro> findByEstadoOrderByFechaSolicitudAsc(
            String estado
    );

    Optional<SolicitudRegistro> findByIdSolicitudAndEstado(Integer idSolicitud, String estado);

    boolean existsByCedulaAndEstadoIn(String cedula, List<String> estados);
}
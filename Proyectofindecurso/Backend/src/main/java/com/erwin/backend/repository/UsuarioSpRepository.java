package com.erwin.backend.repository;

import com.erwin.backend.dtos.UsuarioAdminDto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class UsuarioSpRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public List<UsuarioAdminDto> listarUsuarios() {
        List<Object[]> rows = entityManager.createNativeQuery(
                        "SELECT id_usuario, username, nombres, apellidos, rol, activo FROM sp_listar_usuarios()")
                .getResultList();

        List<UsuarioAdminDto> result = new ArrayList<>();
        for (Object[] row : rows) {
            result.add(new UsuarioAdminDto(
                    ((Number) row[0]).intValue(),
                    (String) row[1],
                    (String) row[2],
                    (String) row[3],
                    (String) row[4],
                    (Boolean) row[5]
            ));
        }
        return result;
    }

    public Integer crearUsuario(String cedula,
                                String correo,
                                String username,
                                String password,
                                String nombres,
                                String apellidos,
                                String rol,
                                Boolean activo) {
        Object result = entityManager.createNativeQuery(
                        "SELECT sp_crear_usuario(?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8)")
                .setParameter(1, cedula)
                .setParameter(2, correo)
                .setParameter(3, username)
                .setParameter(4, password)
                .setParameter(5, nombres)
                .setParameter(6, apellidos)
                .setParameter(7, rol)
                .setParameter(8, activo)
                .getSingleResult();
        return ((Number) result).intValue();
    }

    public void editarUsuario(Integer id,
                              String nombres,
                              String apellidos,
                              String rol,
                              Boolean activo,
                              String password) {
        entityManager.createNativeQuery(
                        "SELECT sp_editar_usuario(?1, ?2, ?3, ?4, ?5, ?6)")
                .setParameter(1, id)
                .setParameter(2, nombres)
                .setParameter(3, apellidos)
                .setParameter(4, rol)
                .setParameter(5, activo)
                .setParameter(6, password)
                .getSingleResult();
    }

    public void cambiarEstado(Integer id, Boolean activo) {
        entityManager.createNativeQuery(
                        "SELECT sp_cambiar_estado_usuario(?1, ?2)")
                .setParameter(1, id)
                .setParameter(2, activo)
                .getSingleResult();
    }
}
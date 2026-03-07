
package com.erwin.backend.repository;

import com.erwin.backend.dtos.RolAppDto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Repository
public class RolAppSpRepository {

    @PersistenceContext
    private EntityManager em;

    public List<RolAppDto> listarRolesApp() {
        List<Object[]> rows = em.createNativeQuery(
                "SELECT id_rol_app, nombre, descripcion, activo, permisos FROM sp_listar_roles_app()"
        ).getResultList();

        List<RolAppDto> out = new ArrayList<>();
        for (Object[] r : rows) {
            out.add(new RolAppDto(
                    ((Number) r[0]).intValue(),
                    (String) r[1],
                    (String) r[2],
                    (Boolean) r[3],
                    toStringList(r[4])
            ));
        }
        return out;
    }

    // ================== CREAR ==================
    // ── FIX Error 2: ahora se envía p_id_rol_base como 5° parámetro ──
    public Integer crearRolApp(String nombre, String descripcion, Boolean activo,
                               List<Integer> permisos, Integer idRolBase) {

        String permisosLiteral = toPgIntArrayLiteral(permisos);

        Object result = em.createNativeQuery(
                        "SELECT sp_crear_rol_app(?1, ?2, ?3, CAST(?4 AS int4[]), ?5)"
                )
                .setParameter(1, nombre)
                .setParameter(2, descripcion)
                .setParameter(3, activo)
                .setParameter(4, permisosLiteral)
                .setParameter(5, idRolBase)   // puede ser null si el SP lo acepta
                .getSingleResult();

        return ((Number) result).intValue();
    }

    // ================== EDITAR (RETURNS VOID) ==================
    // ── FIX Error 2: ahora se envía p_id_rol_base como 5° parámetro ──
    public void editarRolApp(Integer id, String nombre, String descripcion,
                             Boolean activo, Integer idRolBase) {
        // RETURNS VOID — usar executeUpdate() en vez de getSingleResult()
        // para evitar NoResultException de Hibernate
        em.createNativeQuery("SELECT sp_editar_rol_app(?1, ?2, ?3, ?4, ?5)")
                .setParameter(1, id)
                .setParameter(2, nombre)
                .setParameter(3, descripcion)
                .setParameter(4, activo)
                .setParameter(5, idRolBase)
                .getSingleResult();
    }

    // ================== CAMBIAR ESTADO (RETURNS VOID) ==================
    public void cambiarEstadoRolApp(Integer id, Boolean activo) {
        em.createNativeQuery("SELECT sp_cambiar_estado_rol_app(?1, ?2)")
                .setParameter(1, id)
                .setParameter(2, activo)
                .getSingleResult();
    }

    // ================== ASIGNAR PERMISOS ==================
    // ── FIX Error 4: el SP retorna void, wrappear en SELECT para que
    //    Hibernate no lance NoResultException ──
    public void asignarPermisosRolApp(Integer id, List<Integer> permisos) {
        String permisosLiteral = toPgIntArrayLiteral(permisos);

        // Llamada como función: SELECT retorna void (null row),
        // getSingleResult() lanza NoResultException en algunas versiones.
        // Solución: envolver en una expresión que retorne 1.
        em.createNativeQuery(
                        "SELECT sp_asignar_permisos_rol_app(?1, CAST(?2 AS int4[])), 1 AS ok"
                )
                .setParameter(1, id)
                .setParameter(2, permisosLiteral)
                .getSingleResult();
    }

    // ================== helpers ==================

    /**
     * Convierte List<Integer> a literal Postgres int[]: "{1,2,3}"
     * Retorna null si la lista está vacía (el SP lanzará excepción descriptiva).
     */
    private String toPgIntArrayLiteral(List<Integer> permisos) {
        if (permisos == null || permisos.isEmpty()) return null;

        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < permisos.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(permisos.get(i));
        }
        sb.append("}");
        return sb.toString();
    }

    private List<String> toStringList(Object arrayValue) {
        if (arrayValue == null) return Collections.emptyList();

        try {
            if (arrayValue instanceof java.sql.Array sqlArray) {
                Object raw = sqlArray.getArray();
                if (raw instanceof Object[] arr) {
                    List<String> out = new ArrayList<>();
                    for (Object v : arr) out.add(String.valueOf(v));
                    return out;
                }
            }
            if (arrayValue instanceof String[] arr) return Arrays.asList(arr);
            if (arrayValue instanceof Object[] arr) {
                List<String> out = new ArrayList<>();
                for (Object v : arr) out.add(String.valueOf(v));
                return out;
            }
            if (arrayValue instanceof List<?> list) {
                List<String> out = new ArrayList<>();
                for (Object v : list) out.add(String.valueOf(v));
                return out;
            }
            return Collections.singletonList(String.valueOf(arrayValue));
        } catch (SQLException e) {
            throw new RuntimeException("No se pudo leer array permisos", e);
        }
    }
}
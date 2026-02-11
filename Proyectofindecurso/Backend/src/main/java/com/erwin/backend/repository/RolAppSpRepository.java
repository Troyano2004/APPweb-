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

    public Integer crearRolApp(String nombre, String descripcion, Boolean activo, List<Integer> permisos) {

        // En Hibernate 7 / Spring Boot 4, lo más estable es mandar literal y castear a int4[]
        String permisosLiteral = toPgIntArrayLiteral(permisos); // "{1,2,3}"

        Object result = em.createNativeQuery(
                        "SELECT sp_crear_rol_app(?1, ?2, ?3, CAST(?4 AS int4[]))"
                )
                .setParameter(1, nombre)
                .setParameter(2, descripcion)
                .setParameter(3, activo)
                .setParameter(4, permisosLiteral)
                .getSingleResult();

        return ((Number) result).intValue();
    }

    // ================== EDITAR (RETURNS VOID) ==================

    public void editarRolApp(Integer id, String nombre, String descripcion, Boolean activo) {
        // RETURNS VOID => llamar con SELECT y consumir resultado
        em.createNativeQuery("SELECT sp_editar_rol_app(?1, ?2, ?3, ?4)")
                .setParameter(1, id)
                .setParameter(2, nombre)
                .setParameter(3, descripcion)
                .setParameter(4, activo)
                .getSingleResult();
    }

    // ================== CAMBIAR ESTADO (RETURNS VOID) ==================

    public void cambiarEstadoRolApp(Integer id, Boolean activo) {
        em.createNativeQuery("SELECT sp_cambiar_estado_rol_app(?1, ?2)")
                .setParameter(1, id)
                .setParameter(2, activo)
                .getSingleResult();
    }

    // ================== ASIGNAR PERMISOS (RETURNS VOID, recibe int4[]) ==================

    public void asignarPermisosRolApp(Integer id, List<Integer> permisos) {
        String permisosLiteral = toPgIntArrayLiteral(permisos);

        em.createNativeQuery("SELECT sp_asignar_permisos_rol_app(?1, CAST(?2 AS int4[]))")
                .setParameter(1, id)
                .setParameter(2, permisosLiteral)
                .getSingleResult();
    }

    // ================== helpers ==================

    /**
     * Convierte List<Integer> a literal Postgres int[]: "{1,2,3}"
     * (Si está vacío/null devuelve null; el SP valida y lanza error)
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
            // Caso 1: java.sql.Array (Postgres)
            if (arrayValue instanceof java.sql.Array sqlArray) {
                Object raw = sqlArray.getArray();
                if (raw instanceof Object[] arr) {
                    List<String> out = new ArrayList<>();
                    for (Object v : arr) out.add(String.valueOf(v));
                    return out;
                }
            }

            // Caso 2: String[]
            if (arrayValue instanceof String[] arr) return Arrays.asList(arr);

            // Caso 3: Object[]
            if (arrayValue instanceof Object[] arr) {
                List<String> out = new ArrayList<>();
                for (Object v : arr) out.add(String.valueOf(v));
                return out;
            }

            // Caso 4: List
            if (arrayValue instanceof List<?> list) {
                List<String> out = new ArrayList<>();
                for (Object v : list) out.add(String.valueOf(v));
                return out;
            }

            // Fallback
            return Collections.singletonList(String.valueOf(arrayValue));

        } catch (SQLException e) {
            throw new RuntimeException("No se pudo leer array permisos", e);
        }
    }
}
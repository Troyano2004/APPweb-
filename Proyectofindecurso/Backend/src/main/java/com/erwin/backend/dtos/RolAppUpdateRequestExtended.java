package com.erwin.backend.dtos;

/**
 * Extiende RolAppUpdateRequest para incluir idRolBase en la edición.
 * Úsalo en el controller cuando el frontend envíe idRolBase en el body del PUT.
 */
public class RolAppUpdateRequestExtended extends RolAppUpdateRequest {

    // ✅ NUEVO: id del rol base de BD al editar
    private Integer idRolBase;

    public Integer getIdRolBase() { return idRolBase; }
    public void setIdRolBase(Integer idRolBase) { this.idRolBase = idRolBase; }
}

package com.erwin.backend.dtos;

import java.util.List;

public class LoginResponse {

    private Integer      idUsuario;
    private String       rol;        // primer rol (compatibilidad)
    private List<String> roles;      // ✅ NUEVO: todos los roles del usuario
    private String       nombres;
    private String       apellidos;
    private String       token;

    public LoginResponse(Integer idUsuario,
                         String rol,
                         List<String> roles,
                         String nombres,
                         String apellidos,
                         String token) {
        this.idUsuario = idUsuario;
        this.rol       = rol;
        this.roles     = roles;
        this.nombres   = nombres;
        this.apellidos = apellidos;
        this.token     = token;
    }

    public Integer      getIdUsuario() { return idUsuario; }
    public String       getRol()       { return rol;       }
    public List<String> getRoles()     { return roles;     }
    public String       getNombres()   { return nombres;   }
    public String       getApellidos() { return apellidos; }
    public String       getToken()     { return token;     }
}
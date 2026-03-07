
package com.erwin.backend.dtos;

public class LoginResponse {

    private Integer idUsuario;
    private String  rol;
    private String  nombres;
    private String  apellidos;
    private String  token;   // ✅ FIX: JWT con db_user/db_pass embebidos

    public LoginResponse(Integer idUsuario, String rol,
                         String nombres, String apellidos,
                         String token) {
        this.idUsuario = idUsuario;
        this.rol       = rol;
        this.nombres   = nombres;
        this.apellidos = apellidos;
        this.token     = token;
    }

    public Integer getIdUsuario() { return idUsuario; }
    public String  getRol()       { return rol;       }
    public String  getNombres()   { return nombres;   }
    public String  getApellidos() { return apellidos; }
    public String  getToken()     { return token;     }
}
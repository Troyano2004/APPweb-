package com.erwin.backend.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ConfiguracionCorreoDto {
    private Integer id;
    private String proveedor;
    private String usuario;
    private String password; // en texto plano al recibir/enviar, se encripta al guardar
    private Boolean activo;
    // OAuth2 Outlook
    private String clientId;
    private String clientSecret;
    private Boolean autorizado;
}
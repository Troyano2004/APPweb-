package com.erwin.backend.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class BackupTestConexionRequest {

    private String  host;
    private Integer port;
    private String  usuario;
    private String  password;
}
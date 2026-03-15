package com.erwin.backend.dtos;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class ZoomConfigDto {
    private Integer id;
    private String accountId;
    private String clientId;
    private String clientSecret;
    private Boolean configurado;
}
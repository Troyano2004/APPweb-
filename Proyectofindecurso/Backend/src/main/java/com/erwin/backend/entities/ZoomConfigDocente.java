package com.erwin.backend.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "zoom_config_docente")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class ZoomConfigDocente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne
    @JoinColumn(name = "id_docente", nullable = false, unique = true)
    private Docente docente;

    @Column(name = "account_id")
    private String accountId;

    @Column(name = "client_id")
    private String clientId;

    @Column(name = "client_secret")
    private String clientSecret;
}
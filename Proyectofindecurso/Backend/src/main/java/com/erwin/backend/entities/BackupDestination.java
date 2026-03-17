package com.erwin.backend.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "backup_destinations")
public class BackupDestination {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_destination")
    private Long idDestination;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_job", nullable = false)
    private BackupJob job;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 20)
    private TipoDestino tipo;

    @Column(name = "activo")
    private Boolean activo = true;

    @Column(name = "ruta_local", length = 500)
    private String rutaLocal;

    @Column(name = "azure_account", length = 200)
    private String azureAccount;

    @Column(name = "azure_key_enc", columnDefinition = "TEXT")
    private String azureKeyEnc;

    @Column(name = "azure_container", length = 200)
    private String azureContainer;

    @Column(name = "gdrive_cuenta", length = 200)
    private String gdriveCuenta;

    @Column(name = "gdrive_refresh_token_enc", columnDefinition = "TEXT")
    private String gdriveRefreshTokenEnc;

    @Column(name = "gdrive_folder_id", length = 200)
    private String gdriveFolderId;

    @Column(name = "gdrive_folder_nombre", length = 200)
    private String gdriveFolderNombre;

    @Column(name = "s3_bucket", length = 200)
    private String s3Bucket;

    @Column(name = "s3_region", length = 50)
    private String s3Region;

    @Column(name = "s3_access_key", length = 200)
    private String s3AccessKey;

    @Column(name = "s3_secret_key_enc", columnDefinition = "TEXT")
    private String s3SecretKeyEnc;

    @Column(name = "retencion_meses")
    private Integer retencionMeses = 0;

    @Column(name = "retencion_dias")
    private Integer retencionDias = 0;

    @Column(name = "max_backups")
    private Integer maxBackups = 0;

    public enum TipoDestino {
        LOCAL, AZURE, GOOGLE_DRIVE, S3
    }
}
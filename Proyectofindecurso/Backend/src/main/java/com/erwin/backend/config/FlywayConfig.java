package com.erwin.backend.config;

import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class FlywayConfig {

    private static final Logger log = LoggerFactory.getLogger(FlywayConfig.class);

    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    @Value("${spring.datasource.username}")
    private String datasourceUsername;

    @Value("${spring.datasource.password}")
    private String datasourcePassword;

    /**
     * Sin initMethod — controlamos nosotros cuándo llamar a migrate().
     * Si la BD no existe, creamos el bean pero NO migramos.
     * Si la BD existe, migramos normalmente.
     */
    @Bean
    public Flyway flyway(DataSource dataSource) {
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .baselineOnMigrate(true)
                .validateOnMigrate(false)          // evita errores si hay migraciones futuras
                .ignoreMigrationPatterns("*:missing", "*:future")
                .load();

        if (!DataSourceConfig.baseDeDatosExiste(datasourceUrl, datasourceUsername, datasourcePassword)) {
            log.warn("╔══════════════════════════════════════════════════════════╗");
            log.warn("║  RECOVERY MODE — Flyway NO ejecutará migraciones         ║");
            log.warn("║  BD '{}' no existe. Restaura en /recovery.html", extraerNombreBd());
            log.warn("╚══════════════════════════════════════════════════════════╝");
            // NO llamamos flyway.migrate() — retornamos el bean sin migrar
            return flyway;
        }

        log.info("Flyway: BD disponible, ejecutando migraciones...");
        flyway.migrate();
        return flyway;
    }

    private String extraerNombreBd() {
        if (datasourceUrl == null) return "desconocida";
        return datasourceUrl.substring(datasourceUrl.lastIndexOf('/') + 1);
    }
}
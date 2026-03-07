package com.erwin.backend.config;

import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class PeriodoTitulacionTriggerInitializer {

    private final JdbcTemplate jdbcTemplate;

    public PeriodoTitulacionTriggerInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void ensureSingleActivePeriodoTrigger() {
        jdbcTemplate.execute("""
            CREATE OR REPLACE FUNCTION fn_validar_unico_periodo_activo()
            RETURNS TRIGGER AS $$
            BEGIN
                IF NEW.activo = TRUE THEN
                    IF EXISTS (
                        SELECT 1
                        FROM periodo_titulacion pt
                        WHERE pt.activo = TRUE
                          AND (NEW.id_periodo IS NULL OR pt.id_periodo <> NEW.id_periodo)
                    ) THEN
                        RAISE EXCEPTION 'No se permite más de un período académico activo.';
                    END IF;
                END IF;
                RETURN NEW;
            END;
            $$ LANGUAGE plpgsql;
            """);

        jdbcTemplate.execute("""
            DROP TRIGGER IF EXISTS trg_validar_unico_periodo_activo
            ON periodo_titulacion;
            """);

        jdbcTemplate.execute("""
            CREATE TRIGGER trg_validar_unico_periodo_activo
            BEFORE INSERT OR UPDATE
            ON periodo_titulacion
            FOR EACH ROW
            EXECUTE FUNCTION fn_validar_unico_periodo_activo();
            """);
    }
}
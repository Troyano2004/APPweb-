CREATE OR REPLACE FUNCTION fn_audit_trigger()
RETURNS TRIGGER AS $$
DECLARE
    v_accion VARCHAR(50);
    v_config_id INTEGER;
BEGIN
    IF TG_OP = 'INSERT' THEN v_accion := 'CREATE';
    ELSIF TG_OP = 'UPDATE' THEN v_accion := 'UPDATE';
    ELSIF TG_OP = 'DELETE' THEN v_accion := 'DELETE';
    END IF;

    SELECT id INTO v_config_id
    FROM public.audit_config
    WHERE entidad ILIKE TG_TABLE_NAME AND accion = v_accion AND activo = true
    LIMIT 1;

    INSERT INTO public.audit_log (
        config_id, entidad, accion, username,
        ip_address, estado_anterior, estado_nuevo, timestamp_evento
    ) VALUES (
        v_config_id,
        TG_TABLE_NAME,
        v_accion,
        'DB:' || current_user,
        'acceso-directo-bd',
        CASE WHEN TG_OP IN ('UPDATE','DELETE') THEN row_to_json(OLD)::jsonb ELSE NULL END,
        CASE WHEN TG_OP IN ('INSERT','UPDATE') THEN row_to_json(NEW)::jsonb ELSE NULL END,
        NOW()
    );

    IF TG_OP = 'DELETE' THEN RETURN OLD; ELSE RETURN NEW; END IF;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_audit ON public.acta_corte; CREATE TRIGGER trg_audit AFTER INSERT OR UPDATE OR DELETE ON public.acta_corte FOR EACH ROW EXECUTE FUNCTION fn_audit_trigger();
DROP TRIGGER IF EXISTS trg_audit ON public.acta_grado; CREATE TRIGGER trg_audit AFTER INSERT OR UPDATE OR DELETE ON public.acta_grado FOR EACH ROW EXECUTE FUNCTION fn_audit_trigger();
DROP TRIGGER IF EXISTS trg_audit ON public.acta_revision_director; CREATE TRIGGER trg_audit AFTER INSERT OR UPDATE OR DELETE ON public.acta_revision_director FOR EACH ROW EXECUTE FUNCTION fn_audit_trigger();
DROP TRIGGER IF EXISTS trg_audit ON public.anteproyecto_titulacion; CREATE TRIGGER trg_audit AFTER INSERT OR UPDATE OR DELETE ON public.anteproyecto_titulacion FOR EACH ROW EXECUTE FUNCTION fn_audit_trigger();
DROP TRIGGER IF EXISTS trg_audit ON public.anteproyecto_version; CREATE TRIGGER trg_audit AFTER INSERT OR UPDATE OR DELETE ON public.anteproyecto_version FOR EACH ROW EXECUTE FUNCTION fn_audit_trigger();
DROP TRIGGER IF EXISTS trg_audit ON public.antiplagio_intento; CREATE TRIGGER trg_audit AFTER INSERT OR UPDATE OR DELETE ON public.antiplagio_intento FOR EACH ROW EXECUTE FUNCTION fn_audit_trigger();
DROP TRIGGER IF EXISTS trg_audit ON public.asesoria_director; CREATE TRIGGER trg_audit AFTER INSERT OR UPDATE OR DELETE ON public.asesoria_director FOR EACH ROW EXECUTE FUNCTION fn_audit_trigger();
DROP TRIGGER IF EXISTS trg_audit ON public.aval_director; CREATE TRIGGER trg_audit AFTER INSERT OR UPDATE OR DELETE ON public.aval_director FOR EACH ROW EXECUTE FUNCTION fn_audit_trigger();
DROP TRIGGER IF EXISTS trg_audit ON public.banco_temas; CREATE TRIGGER trg_audit AFTER INSERT OR UPDATE OR DELETE ON public.banco_temas FOR EACH ROW EXECUTE FUNCTION fn_audit_trigger();
DROP TRIGGER IF EXISTS trg_audit ON public.bitacora_asignacion; CREATE TRIGGER trg_audit AFTER INSERT OR UPDATE OR DELETE ON public.bitacora_asignacion FOR EACH ROW EXECUTE FUNCTION fn_audit_trigger();
DROP TRIGGER IF EXISTS trg_audit ON public.carrera; CREATE TRIGGER trg_audit AFTER INSERT OR UPDATE OR DELETE ON public.carrera FOR EACH ROW EXECUTE FUNCTION fn_audit_trigger();
DROP TRIGGER IF EXISTS trg_audit ON public.carrera_modalidad; CREATE TRIGGER trg_audit AFTER INSERT OR UPDATE OR DELETE ON public.carrera_modalidad FOR EACH ROW EXECUTE FUNCTION fn_audit_trigger();
DROP TRIGGER IF EXISTS trg_audit ON public.cierre_titulacion; CREATE TRIGGER trg_audit AFTER INSERT OR UPDATE OR DELETE ON public.cierre_titulacion FOR EACH ROW EXECUTE FUNCTION fn_audit_trigger();
DROP TRIGGER IF EXISTS trg_audit ON public.comision_formativa; CREATE TRIGGER trg_audit AFTER INSERT OR UPDATE OR DELETE ON public.comision_formativa FOR EACH ROW EXECUTE FUNCTION fn_audit_trigger();
DROP TRIGGER IF EXISTS trg_audit ON public.comision_miembro; CREATE TRIGGER trg_audit AFTER INSERT OR UPDATE OR DELETE ON public.comision_miembro FOR EACH ROW EXECUTE FUNCTION fn_audit_trigger();
DROP TRIGGER IF EXISTS trg_audit ON public.comision_proyecto; CREATE TRIGGER trg_audit AFTER INSERT OR UPDATE OR DELETE ON public.comision_proyecto FOR EACH ROW EXECUTE FUNCTION fn_audit_trigger();
DROP TRIGGER IF EXISTS trg_audit ON public.complexivo_asesoria; CREATE TRIGGER trg_audit AFTER INSERT OR UPDATE OR DELETE ON public.complexivo_asesoria FOR EACH ROW EXECUTE FUNCTION fn_audit_trigger();
DROP TRIGGER IF EXISTS trg_audit ON public.complexivo_informe_practico; CREATE TRIGGER trg_audit AFTER INSERT OR UPDATE OR DELETE ON public.complexivo_informe_practico FOR EACH ROW EXECUTE FUNCTION fn_audit_trigger();
DROP TRIGGER IF EXISTS trg_audit ON public.complexivo_titulacion; CREATE TRIGGER trg_audit AFTER INSERT OR UPDATE OR DELETE ON public.complexivo_titulacion FOR EACH ROW EXECUTE FUNCTION fn_audit_trigger();
DROP TRIGGER IF EXISTS trg_audit ON public.configuracion_correo; CREATE TRIGGER trg_audit AFTER INSERT OR UPDATE OR DELETE ON public.configuracion_correo FOR EACH ROW EXECUTE FUNCTION fn_audit_trigger();
DROP TRIGGER IF EXISTS trg_audit ON public.coordinador; CREATE TRIGGER trg_audit AFTER INSERT OR UPDATE OR DELETE ON public.coordinador FOR EACH ROW EXECUTE FUNCTION fn_audit_trigger();
DROP TRIGGER IF EXISTS trg_audit ON public.dictamen_propuesta; CREATE TRIGGER trg_audit AFTER INSERT OR UPDATE OR DELETE ON public.dictamen_propuesta FOR EACH ROW EXECUTE FUNCTION fn_audit_trigger();
DROP TRIGGER IF EXISTS trg_audit ON public.docente; CREATE TRIGGER trg_audit AFTER INSERT OR UPDATE OR DELETE ON public.docente FOR EACH ROW EXECUTE FUNCTION fn_audit_trigger();
DROP TRIGGER IF EXISTS trg_audit ON public.docente_carrera; CREATE TRIGGER trg_audit AFTER INSERT OR UPDATE OR DELETE ON public.docente_carrera FOR EACH ROW EXECUTE FUNCTION fn_audit_trigger();
DROP TRIGGER IF EXISTS trg_audit ON public.documento_previo_sustentacion; CREATE TRIGGER trg_audit AFTER INSERT OR UPDATE OR DELETE ON public.documento_previo_sustentacion FOR EACH ROW EXECUTE FUNCTION fn_audit_trigger();
DROP TRIGGER IF EXISTS trg_audit ON public.documento_titulacion; CREATE TRIGGER trg_audit AFTER INSERT OR UPDATE OR DELETE ON public.documento_titulacion FOR EACH ROW EXECUTE FUNCTION fn_audit_trigger();
DROP TRIGGER IF EXISTS trg_audit ON public.documentos_habilitantes; CREATE TRIGGER trg_audit AFTER INSERT OR UPDATE OR DELETE ON public.documentos_habilitantes FOR EACH ROW EXECUTE FUNCTION fn_audit_trigger();
DROP TRIGGER IF EXISTS trg_audit ON public.dt1_asignacion; CREATE TRIGGER trg_audit AFTER INSERT OR UPDATE OR DELETE ON public.dt1_asignacion FOR EACH ROW EXECUTE FUNCTION fn_audit_trigger();
DROP TRIGGER IF EXISTS trg_audit ON public.dt1_revision; CREATE TRIGGER trg_audit AFTER INSERT OR UPDATE OR DELETE ON public.dt1_revision FOR EACH ROW EXECUTE FUNCTION fn_audit_trigger();
DROP TRIGGER IF EXISTS trg_audit ON public.dt1_tutor_estudiante; CREATE TRIGGER trg_audit AFTER INSERT OR UPDATE OR DELETE ON public.dt1_tutor_estudiante FOR EACH ROW EXECUTE FUNCTION fn_audit_trigger();
DROP TRIGGER IF EXISTS trg_audit ON public.dt2_asignacion; CREATE TRIGGER trg_audit AFTER INSERT OR UPDATE OR DELETE ON public.dt2_asignacion FOR EACH ROW EXECUTE FUNCTION fn_audit_trigger();
DROP TRIGGER IF EXISTS trg_audit ON public.eleccion_titulacion; CREATE TRIGGER trg_audit AFTER INSERT OR UPDATE OR DELETE ON public.eleccion_titulacion FOR EACH ROW EXECUTE FUNCTION fn_audit_trigger();
DROP TRIGGER IF EXISTS trg_audit ON public.estudiante; CREATE TRIGGER trg_audit AFTER INSERT OR UPDATE OR DELETE ON public.estudiante FOR EACH ROW EXECUTE FUNCTION fn_audit_trigger();
DROP TRIGGER IF EXISTS trg_audit ON public.evaluacion_sustentacion; CREATE TRIGGER trg_audit AFTER INSERT OR UPDATE OR DELETE ON public.evaluacion_sustentacion FOR EACH ROW EXECUTE FUNCTION fn_audit_trigger();
DROP TRIGGER IF EXISTS trg_audit ON public.examen_complexivo; CREATE TRIGGER trg_audit AFTER INSERT OR UPDATE OR DELETE ON public.examen_complexivo FOR EACH ROW EXECUTE FUNCTION fn_audit_trigger();
DROP TRIGGER IF EXISTS trg_audit ON public.facultad; CREATE TRIGGER trg_audit AFTER INSERT OR UPDATE OR DELETE ON public.facultad FOR EACH ROW EXECUTE FUNCTION fn_audit_trigger();
DROP TRIGGER IF EXISTS trg_audit ON public.login_aplicativo; CREATE TRIGGER trg_audit AFTER INSERT OR UPDATE OR DELETE ON public.login_aplicativo FOR EACH ROW EXECUTE FUNCTION fn_audit_trigger();
DROP TRIGGER IF EXISTS trg_audit ON public.login_bd; CREATE TRIGGER trg_audit AFTER INSERT OR UPDATE OR DELETE ON public.login_bd FOR EACH ROW EXECUTE FUNCTION fn_audit_trigger();
DROP TRIGGER IF EXISTS trg_audit ON public.login_bd_rol; CREATE TRIGGER trg_audit AFTER INSERT OR UPDATE OR DELETE ON public.login_bd_rol FOR EACH ROW EXECUTE FUNCTION fn_audit_trigger();
DROP TRIGGER IF EXISTS trg_audit ON public.modalidad_titulacion; CREATE TRIGGER trg_audit AFTER INSERT OR UPDATE OR DELETE ON public.modalidad_titulacion FOR EACH ROW EXECUTE FUNCTION fn_audit_trigger();
DROP TRIGGER IF EXISTS trg_audit ON public.nota_director_corte; CREATE TRIGGER trg_audit AFTER INSERT OR UPDATE OR DELETE ON public.nota_director_corte FOR EACH ROW EXECUTE FUNCTION fn_audit_trigger();
DROP TRIGGER IF EXISTS trg_audit ON public.observacion_administrativa; CREATE TRIGGER trg_audit AFTER INSERT OR UPDATE OR DELETE ON public.observacion_administrativa FOR EACH ROW EXECUTE FUNCTION fn_audit_trigger();
DROP TRIGGER IF EXISTS trg_audit ON public.observacion_documento; CREATE TRIGGER trg_audit AFTER INSERT OR UPDATE OR DELETE ON public.observacion_documento FOR EACH ROW EXECUTE FUNCTION fn_audit_trigger();
DROP TRIGGER IF EXISTS trg_audit ON public.periodo_titulacion; CREATE TRIGGER trg_audit AFTER INSERT OR UPDATE OR DELETE ON public.periodo_titulacion FOR EACH ROW EXECUTE FUNCTION fn_audit_trigger();
DROP TRIGGER IF EXISTS trg_audit ON public.propuesta_titulacion; CREATE TRIGGER trg_audit AFTER INSERT OR UPDATE OR DELETE ON public.propuesta_titulacion FOR EACH ROW EXECUTE FUNCTION fn_audit_trigger();
DROP TRIGGER IF EXISTS trg_audit ON public.proyecto_documento; CREATE TRIGGER trg_audit AFTER INSERT OR UPDATE OR DELETE ON public.proyecto_documento FOR EACH ROW EXECUTE FUNCTION fn_audit_trigger();
DROP TRIGGER IF EXISTS trg_audit ON public.proyecto_equipo; CREATE TRIGGER trg_audit AFTER INSERT OR UPDATE OR DELETE ON public.proyecto_equipo FOR EACH ROW EXECUTE FUNCTION fn_audit_trigger();
DROP TRIGGER IF EXISTS trg_audit ON public.proyecto_titulacion; CREATE TRIGGER trg_audit AFTER INSERT OR UPDATE OR DELETE ON public.proyecto_titulacion FOR EACH ROW EXECUTE FUNCTION fn_audit_trigger();
DROP TRIGGER IF EXISTS trg_audit ON public.roles_sistema; CREATE TRIGGER trg_audit AFTER INSERT OR UPDATE OR DELETE ON public.roles_sistema FOR EACH ROW EXECUTE FUNCTION fn_audit_trigger();
DROP TRIGGER IF EXISTS trg_audit ON public.solicitud_registro; CREATE TRIGGER trg_audit AFTER INSERT OR UPDATE OR DELETE ON public.solicitud_registro FOR EACH ROW EXECUTE FUNCTION fn_audit_trigger();
DROP TRIGGER IF EXISTS trg_audit ON public.sustentacion; CREATE TRIGGER trg_audit AFTER INSERT OR UPDATE OR DELETE ON public.sustentacion FOR EACH ROW EXECUTE FUNCTION fn_audit_trigger();
DROP TRIGGER IF EXISTS trg_audit ON public.sustentacion_reprogramacion; CREATE TRIGGER trg_audit AFTER INSERT OR UPDATE OR DELETE ON public.sustentacion_reprogramacion FOR EACH ROW EXECUTE FUNCTION fn_audit_trigger();
DROP TRIGGER IF EXISTS trg_audit ON public.tipo_trabajo_titulacion; CREATE TRIGGER trg_audit AFTER INSERT OR UPDATE OR DELETE ON public.tipo_trabajo_titulacion FOR EACH ROW EXECUTE FUNCTION fn_audit_trigger();
DROP TRIGGER IF EXISTS trg_audit ON public.tribunal_proyecto; CREATE TRIGGER trg_audit AFTER INSERT OR UPDATE OR DELETE ON public.tribunal_proyecto FOR EACH ROW EXECUTE FUNCTION fn_audit_trigger();
DROP TRIGGER IF EXISTS trg_audit ON public.tutoria_anteproyecto; CREATE TRIGGER trg_audit AFTER INSERT OR UPDATE OR DELETE ON public.tutoria_anteproyecto FOR EACH ROW EXECUTE FUNCTION fn_audit_trigger();
DROP TRIGGER IF EXISTS trg_audit ON public.universidad; CREATE TRIGGER trg_audit AFTER INSERT OR UPDATE OR DELETE ON public.universidad FOR EACH ROW EXECUTE FUNCTION fn_audit_trigger();
DROP TRIGGER IF EXISTS trg_audit ON public.usuario; CREATE TRIGGER trg_audit AFTER INSERT OR UPDATE OR DELETE ON public.usuario FOR EACH ROW EXECUTE FUNCTION fn_audit_trigger();

SELECT COUNT(*) as triggers_creados FROM information_schema.triggers
WHERE trigger_schema = 'public' AND trigger_name = 'trg_audit';
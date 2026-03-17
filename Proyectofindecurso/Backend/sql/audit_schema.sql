CREATE TABLE IF NOT EXISTS public.audit_config (
    id              SERIAL PRIMARY KEY,
    entidad         VARCHAR(100) NOT NULL,
    accion          VARCHAR(50)  NOT NULL,
    activo          BOOLEAN      NOT NULL DEFAULT true,
    notificar_email BOOLEAN      NOT NULL DEFAULT false,
    destinatarios   TEXT[],
    severidad       VARCHAR(20)  NOT NULL DEFAULT 'LOW',
    descripcion     VARCHAR(255),
    fecha_creacion  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    fecha_actualiz  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    UNIQUE(entidad, accion)
);

CREATE TABLE IF NOT EXISTS public.audit_log (
    id               BIGSERIAL    PRIMARY KEY,
    config_id        INTEGER      REFERENCES public.audit_config(id),
    entidad          VARCHAR(100) NOT NULL,
    entidad_id       VARCHAR(100),
    accion           VARCHAR(50)  NOT NULL,
    id_usuario       INTEGER,
    username         VARCHAR(50),
    correo_usuario   VARCHAR(100),
    ip_address       VARCHAR(45),
    estado_anterior  JSONB,
    estado_nuevo     JSONB,
    metadata         JSONB,
    timestamp_evento TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_audit_log_timestamp  ON public.audit_log(timestamp_evento DESC);
CREATE INDEX IF NOT EXISTS idx_audit_log_entidad    ON public.audit_log(entidad);
CREATE INDEX IF NOT EXISTS idx_audit_log_accion     ON public.audit_log(accion);
CREATE INDEX IF NOT EXISTS idx_audit_log_id_usuario ON public.audit_log(id_usuario);
CREATE INDEX IF NOT EXISTS idx_audit_log_config_id  ON public.audit_log(config_id);

CREATE OR REPLACE FUNCTION fn_audit_config_update_ts()
RETURNS TRIGGER AS $$ BEGIN NEW.fecha_actualiz = NOW(); RETURN NEW; END; $$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_audit_config_update_ts ON public.audit_config;
CREATE TRIGGER trg_audit_config_update_ts BEFORE UPDATE ON public.audit_config
FOR EACH ROW EXECUTE FUNCTION fn_audit_config_update_ts();

INSERT INTO public.audit_config (entidad, accion, activo, notificar_email, severidad, descripcion) VALUES
('Usuario','CREATE',true,true,'HIGH','Creación de usuario nuevo'),
('Usuario','UPDATE',true,false,'MEDIUM','Modificación de datos de usuario'),
('Usuario','DELETE',true,true,'CRITICAL','Eliminación de usuario'),
('SolicitudRegistro','CREATE',true,false,'LOW','Nueva solicitud de registro'),
('SolicitudRegistro','UPDATE',true,false,'MEDIUM','Cambio de estado en solicitud'),
('ProyectoTitulacion','CREATE',true,false,'LOW','Registro de nuevo proyecto'),
('ProyectoTitulacion','UPDATE',true,false,'LOW','Actualización de proyecto'),
('Dt1Asignacion','CREATE',true,true,'MEDIUM','Asignación de docente DT1'),
('Dt2Asignacion','CREATE',true,true,'MEDIUM','Asignación de docente DT2'),
('ConfiguracionCorreo','UPDATE',true,true,'HIGH','Cambio en configuración de correo'),
('PeriodoTitulacion','CREATE',true,false,'MEDIUM','Apertura de nuevo período'),
('PeriodoTitulacion','UPDATE',true,false,'MEDIUM','Modificación de período'),
('Login','LOGIN',true,false,'LOW','Inicio de sesión de usuario'),
('DocumentoTitulacion','UPDATE',true,false,'LOW','Actualización de documento')
ON CONFLICT (entidad, accion) DO NOTHING;

INSERT INTO public.audit_config (entidad, accion, activo, notificar_email, severidad, descripcion) VALUES
('AnteproyectoTitulacion', 'CREATE', true,  false, 'LOW',    'Registro de anteproyecto'),
('AnteproyectoTitulacion', 'UPDATE', true,  false, 'LOW',    'Modificación de anteproyecto'),
('TutoriaAnteproyecto',    'CREATE', true,  false, 'LOW',    'Programación de tutoría'),
('Docente',                'CREATE', true,  false, 'MEDIUM', 'Registro de docente'),
('Docente',                'UPDATE', true,  false, 'LOW',    'Modificación de docente'),
('Estudiante',             'CREATE', true,  false, 'LOW',    'Registro de estudiante'),
('TribunalProyecto',       'CREATE', true,  true,  'HIGH',   'Asignación de tribunal'),
('Sustentacion',           'CREATE', true,  false, 'MEDIUM', 'Registro de sustentación'),
('DocumentoHabilitante',   'CREATE', true,  false, 'LOW',    'Subida de documento habilitante'),
('RolSistema',             'UPDATE', true,  true,  'HIGH',   'Cambio en roles del sistema'),
('ActaRevisionTutor',      'CREATE', true,  false, 'LOW',    'Generación de acta de revisión')
ON CONFLICT (entidad, accion) DO NOTHING;
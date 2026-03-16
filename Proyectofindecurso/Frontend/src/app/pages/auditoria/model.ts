export interface AuditConfig {
  id: number;
  entidad: string;
  accion: string;
  activo: boolean;
  notificarEmail: boolean;
  destinatarios: string[] | null;
  severidad: string;
  descripcion: string | null;
}

export interface AuditLog {
  id: number;
  config?: AuditConfig | null;
  entidad: string;
  entidadId: string | null;
  accion: string;
  idUsuario: number | null;
  username: string | null;
  correoUsuario: string | null;
  ipAddress: string | null;
  estadoAnterior: string | null;
  estadoNuevo: string | null;
  metadata: string | null;
  timestampEvento: string;
}

export interface AuditFiltros {
  entidad?: string;
  accion?: string;
  idUsuario?: number;
  desde?: string;
  hasta?: string;
  page: number;
  size: number;
}

export interface AuditStats {
  totalHoy: number;
  totalSemana: number;
  eventosCriticos24h: number;
  topEntidades: { entidad: string; total: number }[];
  topAcciones: { accion: string; total: number }[];
  topUsuarios: { username: string; total: number }[];
}

export const ENTIDADES_SISTEMA = [
  'Usuario', 'Estudiante', 'Docente', 'Coordinador',
  'SolicitudRegistro', 'ProyectoTitulacion', 'AnteproyectoTitulacion',
  'DocumentoTitulacion', 'DocumentoHabilitante',
  'Dt1Asignacion', 'Dt2Asignacion', 'TribunalProyecto',
  'TutoriaAnteproyecto', 'Sustentacion', 'ActaRevisionTutor',
  'PeriodoTitulacion', 'ConfiguracionCorreo', 'RolSistema', 'Login'
];

export const ACCIONES_SISTEMA = [
  'CREATE', 'UPDATE', 'DELETE', 'LOGIN'
];

export const SEVERIDADES = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];
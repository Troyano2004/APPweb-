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
  totalCriticos: number;
  ultimoEvento: string | null;
  topEntidades: { entidad: string; total: number }[];
  topAcciones: { accion: string; total: number }[];
  topUsuarios: { username: string; total: number }[];
}

export const SEVERIDADES = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];

export function traducirAccion(accion: string): string {
  const map: Record<string, string> = {
    'CREATE':               'Crear',
    'UPDATE':               'Modificar',
    'DELETE':               'Eliminar',
    'LOGIN':                'Inicio de sesión',
    'LOGOUT':               'Cierre de sesión',
    'APROBAR':              'Aprobar',
    'RECHAZAR':             'Rechazar',
    'DECISION':             'Decisión',
    'RESTORE':              'Restaurar',
    'UPLOAD':               'Subir archivo',
    'CALIFICAR':            'Calificar',
    'CONSOLIDAR':           'Consolidar',
    'VALIDAR':              'Validar',
    'ACTIVAR':              'Activar',
    'DESACTIVAR':           'Desactivar',
    'ENVIAR_REVISION':      'Enviar a revisión',
    'APROBAR_DIRECTOR':     'Aprobar (Director)',
    'DEVOLVER':             'Devolver con observaciones',
    'SEGUNDA_OPORTUNIDAD':  'Segunda oportunidad',
    'ASIGNAR_DIRECTOR':     'Asignar director',
    'ASIGNAR_PERMISOS':     'Asignar permisos',
    'CAMBIO_ESTADO':        'Cambiar estado',
    'CANCELAR':             'Cancelar',
    'PROGRAMAR':            'Programar',
    'PROGRAMAR_PREDEFENSA': 'Programar predefensa',
    'CALIFICAR_PREDEFENSA': 'Calificar predefensa',
    'SUGERIR':              'Sugerir tema',
    'BORRADOR':             'Guardar borrador',
  };
  return map[accion] ?? accion;
}
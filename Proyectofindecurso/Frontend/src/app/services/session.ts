export interface SessionUser {
  rol?: string;
  [key: string]: unknown;
}

const SESSION_KEY = 'usuario';

export const setSessionUser = (user: SessionUser): void => {
  localStorage.setItem(SESSION_KEY, JSON.stringify(user));
};

export const getSessionUser = (): SessionUser | null => {
  const raw = localStorage.getItem(SESSION_KEY);
  if (!raw) {
    return null;
  }

  try {
    return JSON.parse(raw) as SessionUser;
  } catch {
    localStorage.removeItem(SESSION_KEY);
    return null;
  }
};

export const isAuthenticated = (): boolean => !!getSessionUser();

export const normalizeRole = (rol: unknown): string => String(rol || '').trim().toUpperCase();

export const hasRole = (rol: unknown, ...expectedRoles: string[]): boolean => {
  const normalizedRole = normalizeRole(rol);
  return expectedRoles.map((item) => normalizeRole(item)).includes(normalizedRole);
};

export const getSessionEntityId = (user: SessionUser | null, kind: 'estudiante' | 'docente'): number | null => {
  if (!user) return null;

  const keys =
    kind === 'estudiante'
      ? ['idEstudiante', 'estudianteId', 'id_usuario', 'idUsuario']
      : ['idDocente', 'docenteId', 'id_usuario', 'idUsuario'];

  for (const key of keys) {
    const raw = user[key];
    const parsed = Number(raw);
    if (Number.isFinite(parsed) && parsed > 0) {
      return parsed;
    }
  }

  return null;
};

export const getRoleHomeRoute = (rol: unknown): string | null => {
  const normalizedRole = normalizeRole(rol);

  if (normalizedRole === 'ROLE_ADMIN') return '/app/admin/usuarios';
  if (normalizedRole === 'ROLE_COORDINADOR') return '/app/dashboard';
  if (normalizedRole === 'ROLE_DOCENTE') return '/app/dashboard';
  if (normalizedRole === 'ROLE_ESTUDIANTE') return '/app/dashboard';

  return null;
};


export interface SessionUser {
  rol?:   string;
  roles?: string[];   // ✅ NUEVO: todos los roles
  [key: string]: unknown;
}

const SESSION_KEY = 'usuario';

export const setSessionUser = (user: SessionUser): void => {
  localStorage.setItem(SESSION_KEY, JSON.stringify(user));
};

export const getSessionUser = (): SessionUser | null => {
  const raw = localStorage.getItem(SESSION_KEY);
  if (!raw) return null;
  try {
    return JSON.parse(raw) as SessionUser;
  } catch {
    localStorage.removeItem(SESSION_KEY);
    return null;
  }
};

export const isAuthenticated = (): boolean => !!getSessionUser();

export const normalizeRole = (rol: unknown): string =>
  String(rol || '').trim().toUpperCase();

export const hasRole = (rol: unknown, ...expectedRoles: string[]): boolean => {
  const normalizedRole = normalizeRole(rol);
  return expectedRoles.map(item => normalizeRole(item)).includes(normalizedRole);
};

// ✅ NUEVO: devuelve todos los roles del usuario como array normalizado
// Ejemplo: ["ROLE_COORDINADOR", "ROLE_DOCENTE"]
export const getUserRoles = (): string[] => {
  const user = getSessionUser();
  if (!user) return [];

  // Si el backend devolvió el array "roles"
  if (Array.isArray(user.roles) && user.roles.length > 0) {
    return user.roles.map(r => normalizeRole(r));
  }

  // Fallback: usar el campo "rol" simple
  const single = normalizeRole(user.rol);
  return single ? [single] : [];
};

// ✅ NUEVO: verifica si el usuario tiene AL MENOS UNO de los roles indicados
export const hasAnyRole = (...expectedRoles: string[]): boolean => {
  const userRoles = getUserRoles();
  const normalized = expectedRoles.map(r => normalizeRole(r));
  return userRoles.some(r => normalized.includes(r));
};

export const getSessionEntityId = (
  user: SessionUser | null,
  kind: 'estudiante' | 'docente'
): number | null => {
  if (!user) return null;
  const keys =
    kind === 'estudiante'
      ? ['idEstudiante', 'estudianteId', 'id_usuario', 'idUsuario']
      : ['idDocente', 'docenteId', 'id_usuario', 'idUsuario'];

  for (const key of keys) {
    const raw    = user[key];
    const parsed = Number(raw);
    if (Number.isFinite(parsed) && parsed > 0) return parsed;
  }
  return null;
};

export const getRoleHomeRoute = (rol: unknown): string | null => {
  const userRoles = getUserRoles();

  // Si ya hay roles en sesión, usar el primero para ruta inicial
  const primaryRole = userRoles.length > 0 ? userRoles[0] : normalizeRole(rol);

  if (primaryRole === 'ROLE_ADMIN')        return '/app/admin/usuarios';
  if (primaryRole === 'ROLE_COORDINADOR')  return '/app/dashboard';
  if (primaryRole === 'ROLE_DOCENTE')      return '/app/dashboard';
  if (primaryRole === 'ROLE_ESTUDIANTE')   return '/app/dashboard';

  return null;
};

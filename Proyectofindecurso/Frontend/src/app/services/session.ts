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

export const getRoleHomeRoute = (rol: unknown): string | null => {
  const normalizedRole = String(rol || '').trim().toUpperCase();

  if (normalizedRole === 'ROLE_ADMIN') return '/app/admin/usuarios';
  if (normalizedRole === 'ROLE_DOCENTE') return '/app/dashboard';
  if (normalizedRole === 'ROLE_ESTUDIANTE') return '/app/dashboard';

  return null;
};

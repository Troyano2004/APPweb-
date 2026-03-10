import { HttpInterceptorFn } from '@angular/common/http';

/**
 * Interceptor que adjunta el JWT (con credenciales BD embebidas)
 * en el header Authorization de cada request al backend.
 *
 * El DbSessionFilter del backend lee este token como PRIORIDAD 1
 * para cambiar la conexión al usuario de BD correcto.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const raw = localStorage.getItem('usuario');
  if (!raw) return next(req);

  try {
    const user = JSON.parse(raw);
    const token = user?.token;

    if (token) {
      const cloned = req.clone({
        setHeaders: { Authorization: `Bearer ${token}` }
      });
      return next(cloned);
    }
  } catch {
    // JSON inválido, ignorar
  }

  return next(req);
};

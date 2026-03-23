import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

/**
 * Interceptor funcional de Angular (compatible con provideHttpClient + withInterceptors).
 *
 * 1. Adjunta el JWT en el header Authorization de cada request al backend.
 *    El DbSessionFilter lo usa como PRIORIDAD 1 para cambiar la conexión BD.
 *
 * 2. Si el backend responde 401 con error "sesion_cerrada_por_admin"
 *    (el JwtFilter lo emite cuando el admin cierra la sesión remotamente),
 *    limpia el almacenamiento local y redirige al login automáticamente.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);
  const raw = localStorage.getItem('usuario');

  let cloned = req;

  if (raw) {
    try {
      const token = JSON.parse(raw)?.token;
      if (token) {
        cloned = req.clone({ setHeaders: { Authorization: `Bearer ${token}` } });
      }
    } catch {
      // JSON inválido, ignorar
    }
  }

  return next(cloned).pipe(
    catchError((err) => {
      if (err.status === 401) {
        const errorCode: string = err.error?.error ?? '';

        if (errorCode === 'sesion_cerrada_por_admin') {
          const cerradaPor: string = err.error?.cerradaPor ?? 'el administrador';

          // Notificación visual con barra de progreso antes de redirigir
          const overlay = document.createElement('div');
          overlay.innerHTML = `
            <div style="
              position: fixed; top: 0; left: 0; right: 0; bottom: 0;
              background: rgba(0,0,0,0.7); z-index: 99999;
              display: flex; align-items: center; justify-content: center;
            ">
              <div style="
                background: white; border-radius: 16px; padding: 40px;
                max-width: 400px; width: 90%; text-align: center;
                box-shadow: 0 20px 60px rgba(0,0,0,0.4);
              ">
                <div style="font-size: 56px; margin-bottom: 16px;">🔒</div>
                <h2 style="color: #1a365d; margin: 0 0 8px 0; font-size: 22px; font-family: sans-serif;">
                  Sesión cerrada
                </h2>
                <p style="color: #4a5568; margin: 0 0 6px 0; font-family: sans-serif;">
                  El usuario <strong>${cerradaPor}</strong> ha cerrado tu sesión de forma remota.
                </p>
                <p style="color: #a0aec0; font-size: 13px; margin: 0 0 24px 0; font-family: sans-serif;">
                  Serás redirigido al inicio de sesión en 3 segundos...
                </p>
                <div style="
                  height: 4px; background: #e2e8f0; border-radius: 4px; overflow: hidden;
                ">
                  <div id="barra-progreso" style="
                    height: 100%; background: #276749; border-radius: 4px;
                    width: 100%; transition: width 3s linear;
                  "></div>
                </div>
              </div>
            </div>
          `;
          document.body.appendChild(overlay);

          // Disparar la animación de la barra en el siguiente frame
          setTimeout(() => {
            const barra = document.getElementById('barra-progreso');
            if (barra) barra.style.width = '0%';
          }, 100);

          // Redirigir después de 3 segundos
          setTimeout(() => {
            document.body.removeChild(overlay);
            localStorage.clear();
            sessionStorage.clear();
            router.navigate(['/login']);
          }, 3000);
        }
      }
      return throwError(() => err);
    })
  );
};

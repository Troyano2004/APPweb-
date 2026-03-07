

import { ApplicationConfig } from '@angular/core';
import { provideRouter } from '@angular/router';
import {
  provideHttpClient,
  withInterceptors
} from '@angular/common/http';

import { routes } from './app.routes';
import { authInterceptor } from './interceptor/auth.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes),

    // ✅ FIX: el interceptor adjunta Authorization: Bearer <token> en cada request.
    // El DbSessionFilter del backend lee el token como PRIORIDAD 1 y cambia la
    // conexión al usuario BD del usuario logueado (ej: app_docente1, app_admin...).
    // Sin esto, el backend siempre usa auth_reader (conexión por defecto).
    provideHttpClient(
      withInterceptors([authInterceptor])
    ),
  ],
};

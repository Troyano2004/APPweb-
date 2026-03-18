
import { ApplicationConfig } from '@angular/core';
import { provideRouter, withRouterConfig } from '@angular/router';
import {
  provideHttpClient,
  withInterceptors
} from '@angular/common/http';

import { routes } from './app.routes';
import { authInterceptor } from './interceptor/auth.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    // ✅ ESTA ES LA LÍNEA QUE ARREGLA TODO
    provideRouter(routes, withRouterConfig({ onSameUrlNavigation: 'reload' })),

    provideHttpClient(
      withInterceptors([authInterceptor])
    ),
  ],
};

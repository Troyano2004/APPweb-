
import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { getUserRoles, isAuthenticated } from '../services/session';

export const authGuard: CanActivateFn = () => {
  if (isAuthenticated()) return true;
  return inject(Router).createUrlTree(['/login']);
};

export const loginGuard: CanActivateFn = () => {
  if (!isAuthenticated()) return true;
  return inject(Router).createUrlTree(['/app/dashboard']);
};

// ✅ CORREGIDO: verifica si el usuario tiene AL MENOS UNO de los roles permitidos
// Antes solo chequeaba user.rol (un solo rol), ahora chequea todos los roles del usuario
export const roleGuard = (...allowedRoles: string[]): CanActivateFn => () => {
  if (!isAuthenticated()) {
    return inject(Router).createUrlTree(['/login']);
  }

  const userRoles = getUserRoles(); // ["ROLE_COORDINADOR", "ROLE_DOCENTE", ...]

  const normalizedAllowed = allowedRoles.map(r =>
    r.trim().toUpperCase().startsWith('ROLE_') ? r.trim().toUpperCase() : 'ROLE_' + r.trim().toUpperCase()
  );

  const tienePermiso = userRoles.some(r => normalizedAllowed.includes(r));

  if (tienePermiso) return true;

  return inject(Router).createUrlTree(['/app/dashboard']);
};

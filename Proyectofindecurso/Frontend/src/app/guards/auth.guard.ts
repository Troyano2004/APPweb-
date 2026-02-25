import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { getSessionUser, hasRole, isAuthenticated } from '../services/session';

export const authGuard: CanActivateFn = () => {
  if (isAuthenticated()) {
    return true;
  }

  return inject(Router).createUrlTree(['/login']);
};

export const loginGuard: CanActivateFn = () => {
  if (!isAuthenticated()) {
    return true;
  }

  return inject(Router).createUrlTree(['/app/dashboard']);
};


export const roleGuard = (...allowedRoles: string[]): CanActivateFn => () => {
  if (!isAuthenticated()) {
    return inject(Router).createUrlTree(['/login']);
  }

  const user = getSessionUser();
  if (hasRole(user?.rol, ...allowedRoles)) {
    return true;
  }

  return inject(Router).createUrlTree(['/app/dashboard']);
};

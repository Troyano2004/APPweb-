import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { isAuthenticated } from '../services/session';

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

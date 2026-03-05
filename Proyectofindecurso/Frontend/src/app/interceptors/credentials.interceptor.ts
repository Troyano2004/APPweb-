import { HttpInterceptorFn } from '@angular/common/http';

export const credentialsInterceptor: HttpInterceptorFn = (req, next) => {
  const authReq = req.clone({
    withCredentials: true   // ← esto envía la cookie JSESSIONID en CADA petición
  });
  return next(authReq);
};

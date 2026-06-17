import { HttpInterceptorFn } from '@angular/common/http';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  // El login y el registro no requieren token (y no deben enviar uno vencido)
  const esEndpointPublico = req.url.includes('/auth/login') || req.url.includes('/auth/registro');
  const token = localStorage.getItem('auth_token');

  if (token && !esEndpointPublico) {
    req = req.clone({
      setHeaders: { Authorization: `Bearer ${token}` }
    });
  }
  return next(req);
};

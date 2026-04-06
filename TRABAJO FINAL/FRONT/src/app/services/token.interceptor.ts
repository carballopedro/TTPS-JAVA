// Con este interceptor meto el header Authorization con el JWT en cada request sin repetir lógica.

import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from './auth.service';

// Este interceptor mete el JWT en cada request para no repetir esa lógica en todos los servicios.
export const tokenInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const token = auth.token; // saco el token que tenga guardado el servicio

  if (!token) {
    return next(req); // si no hay sesión, dejo pasar la request sin tocarla
  }

  const authReq = req.clone({
    setHeaders: {
      Authorization: `Bearer ${token}` // agrego el header Authorization con formato Bearer
    }
  });

  return next(authReq);
};

// Este guard chequea si hay token antes de entrar a rutas protegidas; si no hay, te manda al login.

import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';

export const authGuard: CanActivateFn = (_route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  // Si no tengo token guardado mando a /login, de lo contrario dejo pasar a la ruta.
  return auth.isLoggedIn()
    ? true
    : router.createUrlTree(['/login'], {
        queryParams: { redirectTo: state.url }
      });
};

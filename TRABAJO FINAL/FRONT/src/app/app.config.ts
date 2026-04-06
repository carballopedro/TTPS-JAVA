// Config global de Angular: router, HttpClient e interceptores que necesita toda la app.
import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { tokenInterceptor } from './services/token.interceptor';

import { routes } from './app.routes';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(), // capturo errores globales del browser por si se rompe algo
    provideRouter(routes), // registro todas las rutas principales de la app
    provideHttpClient(withInterceptors([tokenInterceptor])) // configuro HttpClient + mi interceptor de token
  ]
};

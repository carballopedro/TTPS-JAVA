// Acá quedan centralizadas todas las rutas principales de la SPA.
import { Routes } from '@angular/router';

import { HomeComponent } from './components/home/home';
import { LoginComponent } from './components/login/login';
import { RegisterComponent } from './components/register/register';
import { PerfilComponent } from './components/perfil/perfil';
import { MascotasListComponent } from './components/mascotas-list/mascotas-list';
import { AcercaComponent } from './components/acerca/acerca';
import { MascotasDetalleComponent } from './components/mascotas-detalle/mascotas-detalle';
import { PublicacionComponent } from './components/publicacion/publicacion';
import { MispublicacionesComponent } from './components/mispublicaciones/mispublicaciones';
import { PublicacionDetalleComponent } from './components/publicacion-detalle/publicacion-detalle';

import { authGuard } from './services/auth.guard';

// Defino cada ruta principal de la app y qué componente debe renderizar.
export const routes: Routes = [
  { path: '', component: HomeComponent }, // página principal con el hero
  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegisterComponent },
  { path: 'mascotas', component: MascotasListComponent },
  { path: 'mascotas/:id', component: MascotasDetalleComponent },
  { path: 'acerca', component: AcercaComponent },
  { path: 'perfil', component: PerfilComponent, canActivate: [authGuard] }, // esta queda protegida por el guard
  { path: 'publicacion', component: PublicacionComponent, canActivate: [authGuard] }, // esta queda protegida por el guard
  { path: 'mis-publicaciones', component: MispublicacionesComponent, canActivate: [authGuard] },
  { path: 'mis-publicaciones/:id', redirectTo: 'publicacion-detalle/:id', pathMatch: 'full' },
  { path: 'publicacion-detalle/:id', component: PublicacionDetalleComponent, canActivate: [authGuard] },
  { path: '**', redirectTo: '', pathMatch: 'full' } // cualquier ruta inválida redirige al home
];

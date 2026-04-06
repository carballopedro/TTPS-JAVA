// Uso este servicio para manejar todo el login:
// guardo el token, traigo el perfil y expongo el estado de sesión.

import { Injectable, signal } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, tap, switchMap } from 'rxjs';

// Credenciales básicas que se mandan al endpoint de login.
export interface LoginCredentials {
  username: string;
  password: string;
}

// Credenciales que se mandan al endpoint de register
export interface RegisterCredentials {
  nombre: string;
  apellido: string;
  email: string;
  password: string;
  telefono: string;
  barrio: string;
  ciudad: string;
  latitud: number;
  longitud: number;
}

// Payload que envío cuando se edita el perfil.
export interface UpdateProfilePayload {
  nombre: string;
  apellido: string;
  email: string;
  password?: string;
  telefono: string;
  barrio: string;
  ciudad: string;
  instagram?: string;
  sitioWeb?: string;
  latitud?: number;
  longitud?: number;
}

// Respuesta del backend con el token y datos mínimos de sesión.
export interface AuthResponse {
  token: string;
  expiresIn: number;
  username: string;
}

// Perfil completo del usuario autenticado que usa el front.
export interface UserProfile {
  id: number;
  nombre: string;
  apellido: string;
  email: string;
  telefono: string;
  barrio: string;
  ciudad: string;
  latitud: number | null;
  longitud: number | null;
  puntos: number | null;
  habilitado: boolean;
  instagram: string | null;
  sitioWeb: string | null;
  descripcion: string | null;
  rol: string | null;
  badges?: string[] | null;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  // URLs del backend que uso para loguear y traer el perfil autenticado.
  private readonly loginUrl = 'http://localhost:8080/TrabajoFinalCarballo/jwt/auth';
  private readonly profileUrl = 'http://localhost:8080/TrabajoFinalCarballo/usuarios/perfil';

  // URL del backend para registrar un nuevo usuario
  private readonly registerUrl = 'http://localhost:8080/TrabajoFinalCarballo/usuarios';
  // Endpoint para editar los datos del usuario logueado.
  private readonly updateProfileUrl = 'http://localhost:8080/TrabajoFinalCarballo/usuarios';

  // Configuro los headers una vez para no repetirlos en cada request.
  private readonly jsonHeaders = new HttpHeaders({
    'Content-Type': 'application/json'
  });
  // Signals donde guardo tanto el usuario logueado como el estado booleano de sesión.
  private readonly currentUserSignal = signal<UserProfile | null>(null);
  private readonly isAuthenticatedSignal = signal(false);
  private readonly storageKey = 'tf_token';
  private readonly userIdKey = 'tf_user_id';
  private readonly currentUserIdSignal = signal<number | null>(null);

  readonly currentUser = this.currentUserSignal.asReadonly();
  readonly isAuthenticated = this.isAuthenticatedSignal.asReadonly();
  readonly userId = this.currentUserIdSignal.asReadonly();

  constructor(private readonly http: HttpClient) {
    this.restoreSession();
  }

  // Acá hago todo el flujo de login: mando credenciales, guardo el token y despacho el perfil.
  login(credentials: LoginCredentials, remember = false): Observable<UserProfile> {
    const payload = {
      username: credentials.username,
      password: credentials.password
    };

    return this.http
      .post<AuthResponse>(this.loginUrl, payload, { headers: this.jsonHeaders })
      .pipe(
        tap(({ token }) => {
          this.persistToken(token, remember); // guardo el token donde corresponda (local o session storage)
          this.isAuthenticatedSignal.set(true); // marco que ya hay sesión activa
        }),
        switchMap(() => this.fetchProfile()), // inmediatamente pido el perfil protegido
        tap((profile) => {
          this.currentUserSignal.set(profile); // guardo el resultado para el resto de la app
          this.persistUserId(profile.id); // persisto el ID para poder reutilizarlo tras refresh
        })
      );
  }

  // Limpio cualquier rastro de la sesión en el navegador.
  logout(): void {
    localStorage.removeItem(this.storageKey);
    sessionStorage.removeItem(this.storageKey);
    localStorage.removeItem(this.userIdKey);
    sessionStorage.removeItem(this.userIdKey);
    this.currentUserSignal.set(null);
    this.currentUserIdSignal.set(null);
    this.isAuthenticatedSignal.set(false);
  }

  // leo el token guardado sin importar si quedó en local o session storage
  get token(): string | null {
    return localStorage.getItem(this.storageKey) ?? sessionStorage.getItem(this.storageKey);
  }

  // helper rápido para saber si hay sesión activa
  isLoggedIn(): boolean {
    return !!this.token;
  }

  private persistToken(token: string, remember: boolean): void {
    // Si el usuario tildó "recordarme" guardo el token en localStorage, caso contrario en sessionStorage.
    if (remember) {
      localStorage.setItem(this.storageKey, token);
      sessionStorage.removeItem(this.storageKey);
    } else {
      sessionStorage.setItem(this.storageKey, token);
      localStorage.removeItem(this.storageKey);
    }
  }

  // Intento reconstruir la sesión del usuario leyendo el token almacenado al inicio de la app.
  private restoreSession(): void {
    const token = this.token;
    this.isAuthenticatedSignal.set(!!token);
    this.restoreStoredUserId(); // intento reconstruir el ID aunque todavía no traiga el perfil
    if (!token) {
      return;
    }
    // Si llego hasta acá vuelvo a pedir el perfil; si falla, fuerzo logout porque el token ya no sirve.
    this.fetchProfileWithHandling();
  }

  // Si un componente necesita el perfil y todavía no se cargó, lo vuelvo a pedir al backend.
  ensureProfileLoaded(): void {
    if (!this.token || this.currentUserSignal()) {
      return;
    }
    this.fetchProfileWithHandling();
  }

  // Fuerza la recarga del perfil desde el backend para tener datos actualizados en vistas como perfil.
  refreshProfile(): void {
    if (!this.token) {
      return;
    }
    this.fetchProfileWithHandling();
  }

  // Centralizo la llamada al endpoint protegido para no duplicar código.
  private fetchProfile(): Observable<UserProfile> {
    return this.http.get<UserProfile>(this.profileUrl);
  }

  // Reutilizo este código para pedir el perfil y manejar los posibles errores en un solo lugar.
  private fetchProfileWithHandling(): void {
    this.fetchProfile().subscribe({
      next: (profile) => {
        this.currentUserSignal.set(profile);
        this.persistUserId(profile.id); // mantengo sincronizado el ID persistido
      },
      error: (error) => {
        // Si el servidor responde que el token ya no sirve, limpio la sesión;
        // pero ante errores de red u otros códigos mantengo el token para no desloguear al usuario.
        if (error?.status === 401 || error?.status === 403) {
          this.logout();
          return;
        }
        console.error('No pudimos recuperar el perfil del usuario.', error);
      }
    });
  }

  // crea un nuevo usuario mandando todos los datos del formulario de registro
  register (credentials: RegisterCredentials): Observable<any> {
      const payload = {
        nombre: credentials.nombre,
        apellido: credentials.apellido,
        email: credentials.email,
        password: credentials.password,
        telefono: credentials.telefono,
        barrio: credentials.barrio,
        ciudad: credentials.ciudad,
        latitud: credentials.latitud,
        longitud: credentials.longitud,
      };
      return this.http.post<any>(this.registerUrl, payload, { headers: this.jsonHeaders });
  }

  updateProfile(userId: number, payload: UpdateProfilePayload): Observable<UserProfile> {
    // Mando los datos modificados y actualizo el signal con el perfil devuelto.
    return this.http.put<UserProfile>(`${this.updateProfileUrl}/${userId}`, payload).pipe(
      tap((profile) => {
        this.currentUserSignal.set(profile);
        this.persistUserId(profile.id); // actualizo también el ID persistido por cualquier cambio
      })
    );
  }

  // Persisto o borro el ID del usuario siguiendo el mismo storage que use para el token.
  private persistUserId(id: number | null): void {
    if (id == null) {
      localStorage.removeItem(this.userIdKey);
      sessionStorage.removeItem(this.userIdKey);
      this.currentUserIdSignal.set(null);
      return;
    }
    const tokenInLocalStorage = !!localStorage.getItem(this.storageKey);
    const preferredStorage = tokenInLocalStorage ? localStorage : sessionStorage;
    const fallbackStorage = tokenInLocalStorage ? sessionStorage : localStorage;
    preferredStorage.setItem(this.userIdKey, String(id));
    fallbackStorage.removeItem(this.userIdKey);
    this.currentUserIdSignal.set(id);
  }

  // Recupero el ID guardado cuando la app arranca y todavía no hice el request del perfil.
  private restoreStoredUserId(): void {
    const stored = localStorage.getItem(this.userIdKey) ?? sessionStorage.getItem(this.userIdKey);
    if (!stored) {
      this.currentUserIdSignal.set(null);
      return;
    }
    const parsed = Number(stored);
    this.currentUserIdSignal.set(Number.isFinite(parsed) ? parsed : null);
  }
}

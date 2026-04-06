import { Component, ChangeDetectionStrategy, ElementRef, HostListener, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { NgIf } from '@angular/common';
import { AuthService } from '../../services/auth.service';

export type AppView = 'dashboard' | 'ranking';

@Component({
  selector: 'app-header',
  templateUrl: './header.html',
  imports: [RouterLink, NgIf],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: true,
  styleUrl: './header.css'
})
export class HeaderComponent {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly host = inject(ElementRef<HTMLElement>);

  protected readonly currentUser = this.auth.currentUser; // signal con los datos del usuario logueado
  protected readonly isLoggedIn = this.auth.isAuthenticated; // flag reactivo para mostrar el menú de cuenta
  protected readonly isMenuOpen = signal(false); // manejo la apertura del dropdown
  protected readonly isConfirmOpen = signal(false); // manejo la ventana modal de confirmación

  // cierro sesión, reseteo los menús y vuelvo al inicio
  protected confirmLogout() {
    this.auth.logout(); // borro la sesión desde el servicio
    this.isMenuOpen.set(false);
    this.isConfirmOpen.set(false);
    this.router.navigateByUrl('/');
  }

  // abre o cierra el menú de usuario según el estado actual
  protected toggleMenu() {
    this.isMenuOpen.update((value) => !value);
  }

  // fuerza el cierre del menú desplegable
  protected closeMenu() {
    this.isMenuOpen.set(false);
  }

  // muestro el modal para confirmar la salida
  protected openConfirm() {
    this.isConfirmOpen.set(true);
  }

  // cancelo la acción de logout y cierro el modal
  protected cancelLogout() {
    this.isConfirmOpen.set(false);
  }

  @HostListener('document:click', ['$event'])
  // si hago click fuera del header cierro el menú
  protected onDocumentClick(event: MouseEvent) {
    if (!this.host.nativeElement.contains(event.target as Node)) {
      this.isMenuOpen.set(false);
    }
  }
}

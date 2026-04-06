// Componente raíz que arma la estructura base y muestra header/footer según la ruta.
import { Component, signal } from '@angular/core';
import { RouterOutlet, Router, NavigationEnd } from '@angular/router';
import { HeaderComponent } from "./components/header/header";
import { FooterComponent } from "./components/footer/footer";
import { filter } from 'rxjs/operators';
import { NgIf } from '@angular/common';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, HeaderComponent, FooterComponent, NgIf],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class AppComponent {
  protected readonly title = signal('trabajoFinal');

  mostrarHeader:boolean = true;
  mostrarFooter:boolean = true;

  constructor(private router: Router) {
    // Escucho los eventos de navegación para saber en qué pantalla estoy.
    this.router.events.pipe(
      filter(event => event instanceof NavigationEnd) // sólo me interesan las navegaciones completadas
    ).subscribe((event: any) => {
      // Si entro a login o register oculto header/footer para enfocarme en el formulario.
      const ocultar = event.url.includes('login') || event.url.includes('register');
      this.mostrarHeader = !ocultar;
      this.mostrarFooter = !ocultar;
      window.scrollTo({ top: 0, behavior: 'smooth' }); // llevo al usuario al inicio en cada cambio de ruta
    });
  }
}

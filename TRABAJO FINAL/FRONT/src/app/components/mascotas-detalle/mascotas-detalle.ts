import { Component, ChangeDetectionStrategy, ElementRef, OnDestroy, ViewChild, ChangeDetectorRef, inject } from '@angular/core';
import { NgIf } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import * as L from 'leaflet';
import { PublicationDetail } from '../../services/mascotas.service';
import { AuthService } from '../../services/auth.service';
import { PublicacionesService } from '../../services/publicaciones.service';

@Component({
  selector: 'app-mascotas-detalle',
  standalone: true,
  imports: [NgIf, RouterLink],
  templateUrl: './mascotas-detalle.html',
  styleUrl: './mascotas-detalle.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
// Vista de detalle que recicla la info del listado y se actualiza si hace falta.
export class MascotasDetalleComponent implements OnDestroy {
  private readonly auth = inject(AuthService);
  private readonly publicaciones = inject(PublicacionesService);
  private readonly cdr = inject(ChangeDetectorRef);
  protected detail?: PublicationDetail;
  protected readonly defaultImage = 'pet-placeholder.svg';
  protected backRoute = '/mascotas';
  protected backLabel = 'Volver al listado';
  // Helpers que normalizan texto y estados para reutilizarlos en la template.
  protected readonly formatLabel = (value?: string | null) => {
    if (!value) {
      return 'Sin datos';
    }
    const sanitized = value.replace(/_/g, ' ');
    const lower = sanitized.toLowerCase();
    const formatted = lower.charAt(0).toUpperCase() + lower.slice(1);
    return formatted.replace('Pequeno', 'Pequeño');
  };
  protected readonly formatPetStatus = (value?: string | null) => {
    if (!value) {
      return undefined;
    }
    const normalized = value.toLowerCase();
    if (normalized === 'perdido_propio') {
      return 'Perdido Propio';
    }
    if (normalized === 'perdido_ajeno') {
      return 'Perdido Ajeno';
    }
    if (normalized === 'recuperado') {
      return 'Recuperado';
    }
    if (normalized === 'adoptado') {
      return 'Adoptado';
    }
    return this.formatLabel(value);
  };

  private map?: L.Map;
  private marker?: L.Marker;
  private mapElement?: HTMLElement;
  protected adoptionPromptVisible = false; // modal para confirmar adopción
  protected recoveryPromptVisible = false; // modal para confirmar recuperar
  protected isUpdatingState = false;
  protected stateChangeError?: string;
  protected stateChangeSuccessMessage?: string;
  private stateChangeSuccessRoute?: string;
  private readonly markerIconUrl =
    'data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="30" height="40" viewBox="0 0 30 40"><path d="M15 0C8 0 2.5 5.5 2.5 12.5C2.5 22 15 40 15 40C15 40 27.5 22 27.5 12.5C27.5 5.5 22 0 15 0Z" fill="%23148851" stroke="%230f6f42" stroke-width="2"/><circle cx="15" cy="13" r="4" fill="white"/></svg>';
  private readonly markerIcon = L.icon({
    iconRetinaUrl: this.markerIconUrl,
    iconUrl: this.markerIconUrl,
    iconSize: [30, 40],
    iconAnchor: [15, 40],
    popupAnchor: [0, -36],
    tooltipAnchor: [0, -36],
    shadowUrl: undefined,
  });

  @ViewChild('detailMap') set detailMap(element: ElementRef<HTMLDivElement> | undefined) {
    this.mapElement = element?.nativeElement;
    if (this.mapElement) {
      setTimeout(() => this.initMap(), 0);
    }
  }

  private fromHome = false;
  private fromList = false;

  // leo el state de la navegación para armar el detalle sin pedirlo de nuevo
  constructor(private readonly router: Router) {
    // Aprovecho el state de la navegación para evitar otra llamada al backend.
    const navigationState = this.router.getCurrentNavigation()?.extras.state as { detail?: PublicationDetail; fromHome?: boolean; fromList?: boolean } | undefined;
    this.detail = navigationState?.detail ?? (history.state?.detail as PublicationDetail | undefined);
    const fromHome = navigationState?.fromHome ?? (history.state?.fromHome as boolean | undefined);
    const fromList = navigationState?.fromList ?? (history.state?.fromList as boolean | undefined);
    this.fromHome = !!fromHome;
    this.fromList = !!fromList && !this.fromHome;
    if (this.fromHome) {
      this.backRoute = '/';
      this.backLabel = 'Volver al inicio';
    } else if (this.fromList) {
      this.backRoute = '/mascotas';
      this.backLabel = 'Volver al listado';
    }
    if (!this.detail) {
      this.router.navigate(['/mascotas']);
    }
  }

  // libero recursos cuando salgo del detalle
  ngOnDestroy(): void {
    this.destroyMap();
  }

  // Aplica un formato uniforme independientemente de cómo llegue la fecha.
  protected formatDate(value: string | number[] | undefined): string {
    if (!value) {
      return 'Sin datos';
    }
    if (Array.isArray(value) && value.length >= 3) {
      const [year, month, day] = value;
      return `${day.toString().padStart(2, '0')}/${month.toString().padStart(2, '0')}/${year}`;
    }
    if (typeof value === 'string') {
      const parts = value.split(/[-/]/);
      if (parts.length === 3) {
        const [year, month, day] = parts;
        return `${day.padStart(2, '0')}/${month.padStart(2, '0')}/${year}`;
      }
    }
    return String(value);
  }

  // Inicializa el mapa solo cuando hay coordenadas disponibles.
  private initMap(): void {
    if (!this.mapElement || this.map || !this.detail) {
      return;
    }
    const { latitud, longitud } = this.detail.publication;
    if (latitud == null || longitud == null) {
      return;
    }
    this.map = L.map(this.mapElement, {
      zoomControl: true,
      scrollWheelZoom: true,
    }).setView([latitud, longitud], 15);

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      attribution: '© OpenStreetMap contributors',
    }).addTo(this.map);

    this.marker = L.marker([latitud, longitud], { icon: this.markerIcon }).addTo(this.map);
  }

  // Limpia listeners y referencias cuando el componente se destruye.
  private destroyMap(): void {
    this.map?.off();
    this.map?.remove();
    this.map = undefined;
    this.marker = undefined;
  }

  // Solo muestro "Adoptar" cuando hay sesión activa, el estado es perdido ajeno y no soy el creador del reporte.
  protected shouldShowAdoptButton(): boolean {
    if (!this.detail) {
      return false;
    }
    const userId = this.auth.userId();
    if (userId == null) {
      return false;
    }
    const state = this.detail.publication.mascota?.estado?.toLowerCase();
    if (state !== 'perdido_ajeno') {
      return false;
    }
    return this.detail.creator.id !== userId;
  }

  // El botón "Recuperar" se muestra bajo las mismas condiciones (perdido ajeno y no soy el creador).
  protected shouldShowRecoverButton(): boolean {
    if (!this.detail) {
      return false;
    }
    const userId = this.auth.userId();
    if (userId == null) {
      return false;
    }
    const state = this.detail.publication.mascota?.estado?.toLowerCase();
    if (state !== 'perdido_ajeno') {
      return false;
    }
    return this.detail.creator.id !== userId;
  }

  // Abro la confirmación de adopción reutilizando el mismo patrón que en el header.
  protected openAdoptPrompt(): void {
    this.stateChangeError = undefined;
    this.adoptionPromptVisible = true;
  }

  // muestro el modal para confirmar recuperación
  protected openRecoverPrompt(): void {
    this.stateChangeError = undefined;
    this.recoveryPromptVisible = true;
  }

  // cierro ambos modales sin hacer cambios
  protected cancelStatePrompt(): void {
    this.adoptionPromptVisible = false;
    this.recoveryPromptVisible = false;
  }

  // disparo la acción de adopción con una sola puerta de entrada
  protected confirmAdopt(): void {
    this.updateState('Adoptar');
  }

  // disparo la acción de recuperación reutilizando la misma lógica
  protected confirmRecover(): void {
    this.updateState('Recuperar');
  }

  // manda la acción al backend y actualiza la UI según el resultado
  private updateState(action: 'Adoptar' | 'Recuperar'): void {
    if (!this.detail || this.isUpdatingState) {
      return;
    }
    this.isUpdatingState = true;
    this.stateChangeError = undefined;
    this.publicaciones.changePublicationState(this.detail.publication.id, action).subscribe({
      next: () => {
        this.isUpdatingState = false;
        this.adoptionPromptVisible = false;
        this.recoveryPromptVisible = false;
        this.stateChangeSuccessMessage =
          action === 'Adoptar'
            ? 'La mascota ha sido adoptada, sumaste 10 puntos.'
            : 'La mascota ha sido recuperada.';
        this.stateChangeSuccessRoute = this.fromHome ? '/' : '/mascotas';
        this.cdr.markForCheck();
      },
      error: () => {
        this.isUpdatingState = false;
        this.stateChangeError = 'No pudimos completar la acción. Intentá nuevamente.';
        this.cdr.markForCheck();
      },
    });
  }

  // cuando acepto el mensaje final me lleva a la vista correspondiente
  protected confirmStateSuccess(): void {
    if (!this.stateChangeSuccessMessage) {
      return;
    }
    const target = this.stateChangeSuccessRoute ?? '/mascotas';
    this.stateChangeSuccessMessage = undefined;
    this.stateChangeSuccessRoute = undefined;
    this.cdr.markForCheck();
    this.router.navigate([target]);
  }
}

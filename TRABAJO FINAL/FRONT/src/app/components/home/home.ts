import { Component, ChangeDetectionStrategy, OnDestroy, OnInit, ViewChild, ElementRef, effect, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { NgIf, NgFor } from '@angular/common';
import { AuthService, UserProfile } from '../../services/auth.service';
import { RankingService, RankedUser } from '../../services/ranking.service';
import { MascotasService, PublicationDetail } from '../../services/mascotas.service';
import * as L from 'leaflet';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [NgIf, NgFor],
  templateUrl: './home.html',
  styleUrl: './home.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class HomeComponent implements OnInit, OnDestroy {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly ranking = inject(RankingService);
  private readonly mascotas = inject(MascotasService);
  protected readonly showLoginPrompt = signal(false);
  protected readonly rankingLoading = signal(true);
  protected readonly rankingError = signal('');
  protected readonly topUsers = signal<RankedUser[]>([]);
  protected readonly recentLoading = signal(true);
  protected readonly recentError = signal('');
  protected readonly recentPublications = signal<PublicationDetail[]>([]);
  protected readonly zoneReports = signal<PublicationDetail[]>([]);
  protected readonly zoneError = signal('');
  protected readonly zoneLoading = signal(false);
  private zoneLeafletMap?: L.Map;
  private zoneMarkers?: L.LayerGroup;
  private zoneMapElement?: HTMLElement;
  private lastZoneUserKey?: string;
  private readonly markerIcon = L.icon({
    iconRetinaUrl:
      'data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="30" height="40" viewBox="0 0 30 40"><path d="M15 0C8 0 2.5 5.5 2.5 12.5C2.5 22 15 40 15 40C15 40 27.5 22 27.5 12.5C27.5 5.5 22 0 15 0Z" fill="%23148851" stroke="%230f6f42" stroke-width="2"/><circle cx="15" cy="13" r="4" fill="white"/></svg>',
    iconUrl:
      'data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="30" height="40" viewBox="0 0 30 40"><path d="M15 0C8 0 2.5 5.5 2.5 12.5C2.5 22 15 40 15 40C15 40 27.5 22 27.5 12.5C27.5 5.5 22 0 15 0Z" fill="%23148851" stroke="%230f6f42" stroke-width="2"/><circle cx="15" cy="13" r="4" fill="white"/></svg>',
    iconSize: [30, 40],
    iconAnchor: [15, 40],
    popupAnchor: [0, -36],
    tooltipAnchor: [0, -36],
  });

  @ViewChild('zoneMap') set zoneMap(element: ElementRef<HTMLDivElement> | undefined) {
    // Guardo el contenedor del mapa y lo inicializo cuando hay datos disponibles.
    this.zoneMapElement = element?.nativeElement;
    if (this.zoneMapElement && this.zoneReports().length) {
      setTimeout(() => this.initZoneMap(), 0);
    }
  }
  private readonly syncZoneMapWithUser = effect(
    () => {
      const profile = this.auth.currentUser();
      const lat = profile?.latitud;
      const lng = profile?.longitud;
      if (profile && lat != null && lng != null) {
        const key = `${profile.id}-${lat}-${lng}`;
        if (this.lastZoneUserKey !== key) {
          this.lastZoneUserKey = key;
          this.loadZoneReports(profile);
        }
        return;
      }
      this.lastZoneUserKey = undefined;
      this.zoneReports.set([]);
      this.zoneLoading.set(false);
      this.zoneError.set('');
      this.destroyZoneMap();
    },
    { allowSignalWrites: true }
  );

  // verifico si hay sesión activa para mostrar acciones limitadas
  protected isLoggedIn(): boolean {
    return this.auth.isLoggedIn();
  }

  // al iniciar cargo perfil, ranking y últimas publicaciones
  ngOnInit(): void {
    this.auth.ensureProfileLoaded(); // me aseguro de traer el perfil tras un refresh antes de usar lat/long
    this.loadRanking();
    this.loadRecentPublications();
  }

  // limpio listeners y el mapa cuando se destruye el componente
  ngOnDestroy(): void {
    this.destroyZoneMap();
  }

  // si estoy logueado mando a crear publicación, sino pido login
  protected onCreatePublication(): void {
    if (this.auth.isLoggedIn()) {
      this.router.navigateByUrl('/publicacion');
      return;
    }
    this.showLoginPrompt.set(true);
  }

  // cierro el modal que invita a loguearse
  protected cancelPrompt(): void {
    this.showLoginPrompt.set(false);
  }

  // redirijo al login y guardo dónde volver
  protected goToLogin(): void {
    this.showLoginPrompt.set(false);
    this.router.navigate(['/login'], {
      queryParams: { redirectTo: '/publicacion' }
    });
  }

  // Lleva al detalle de la publicación reutilizando el mismo flujo que en el listado general.
  // según sea propia o no, mando al detalle correcto
  protected viewRecentDetail(detail: PublicationDetail): void {
    const currentUserId = this.auth.userId();
    if (currentUserId != null && currentUserId === detail.creator.id) {
      // Si la publicación es propia, redirijo al detalle privado para habilitar acciones administrativas.
      this.router.navigate(['/publicacion-detalle', detail.publication.id], {
        state: {
          publication: detail.publication,
          backRoute: '/',
          backLabel: 'Volver al inicio',
        }
      });
      return;
    }
    this.router.navigate(['/mascotas', detail.publication.id], { state: { detail, fromHome: true } });
  }

  // Reutilizo helpers de formato para no duplicar lógica en la template.
  // preparo el estado de la mascota para mostrarlo legible
  protected formatPetStatus(value?: string | null): string {
    if (!value) {
      return 'Estado sin datos';
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
  }

  // normalizo textos reemplazando guiones bajos y capitalizando
  protected formatLabel(value?: string | null): string {
    if (!value) {
      return 'Sin datos';
    }
    const sanitized = value.replace(/_/g, ' ');
    const lower = sanitized.toLowerCase();
    const formatted = lower.charAt(0).toUpperCase() + lower.slice(1);
    return formatted.replace('Pequeno', 'Pequeño');
  }

  // Mapea el badge recibido a su etiqueta legible acompañada del emoji correspondiente.
  // devuelvo el nombre de la medalla con su emoji
  protected formatBadgeLabel(badge?: string | null): string {
    const normalized = badge?.toLowerCase();
    if (normalized === 'bronce') {
      return 'Bronce 🥉';
    }
    if (normalized === 'plata') {
      return 'Plata 🥈';
    }
    if (normalized === 'oro') {
      return 'Oro 🥇';
    }
    return badge ?? 'Sin insignia';
  }

  // convierto fechas heterogéneas en el formato dd/mm/aaaa
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

  // optimizo el *ngFor del home usando el id de publicación
  protected trackByPublicationId(_: number, detail: PublicationDetail): number {
    return detail.publication.id;
  }

  // trae el ranking desde el servicio cuidando estados de carga
  private loadRanking(): void {
    this.rankingLoading.set(true);
    this.rankingError.set('');
    // Consumo el endpoint limitando a los 5 primeros usuarios.
    this.ranking.getTopUsers(6).subscribe({
      next: (users) => {
        const ordered = [...users].sort((a, b) => b.puntos - a.puntos);
        this.topUsers.set(ordered);
        this.rankingLoading.set(false);
      },
      error: () => {
        this.topUsers.set([]);
        this.rankingError.set('No pudimos cargar el ranking.');
        this.rankingLoading.set(false);
      },
    });
  }

  // Obtiene las 6 publicaciones más recientes para mostrarlas en el home.
  // consulta las publicaciones recientes para el carrusel del home
  private loadRecentPublications(): void {
    this.recentLoading.set(true);
    this.recentError.set('');
    this.mascotas.listRecentPublications(6).subscribe({
      next: (items) => {
        this.recentPublications.set(items);
        this.recentLoading.set(false);
      },
      error: () => {
        this.recentPublications.set([]);
        this.recentError.set('No pudimos cargar las publicaciones recientes.');
        this.recentLoading.set(false);
      },
    });
  }

  // carga los reportes cercanos al usuario logueado para pintarlos en el mapa
  private loadZoneReports(user: UserProfile): void {
    this.zoneLoading.set(true);
    this.zoneError.set('');
    this.mascotas.listPublications().subscribe({
      next: (items) => {
        const lostReports = items.filter((detail) => {
          const state = detail.publication.mascota?.estado?.toLowerCase() ?? '';
          return state.includes('perdido');
        });
        this.zoneReports.set(lostReports);
        this.zoneLoading.set(false);
        if (lostReports.length && this.zoneMapElement) {
          setTimeout(() => this.initZoneMap(), 0);
        }
      },
      error: () => {
        this.zoneReports.set([]);
        this.zoneLoading.set(false);
        this.zoneError.set('No pudimos cargar el mapa de tu zona.');
      },
    });
  }

  // inicializa el mapa Leaflet centrado en la posición del usuario
  private initZoneMap(): void {
    if (!this.zoneMapElement || this.zoneLeafletMap) {
      this.updateZoneMarkers();
      return;
    }
    const center = this.getUserCoords() ?? [-34.92145, -57.95453];
    this.zoneLeafletMap = L.map(this.zoneMapElement, {
      zoomControl: true,
      scrollWheelZoom: true,
    }).setView(center, 13);
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      attribution: '© OpenStreetMap contributors',
    }).addTo(this.zoneLeafletMap);
    this.updateZoneMarkers();
  }

  // limpia y vuelve a dibujar los pines del mapa con los datos actuales
  private updateZoneMarkers(): void {
    if (!this.zoneLeafletMap) {
      return;
    }
    this.zoneMarkers?.remove();
    const markers = this.zoneReports()
      .map((detail) => {
        const { latitud, longitud } = detail.publication;
        if (latitud == null || longitud == null) {
          return null;
        }
        const pet = detail.publication.mascota;
        // Reutilizo el mismo diseño compacto que en mascotas-list para mantener consistencia visual.
        const popupHtml = `
          <div class="map-popup" style="display:flex;gap:10px;align-items:center;max-width:260px;">
            <img src="${pet?.fotoUrl || 'pet-placeholder.svg'}" alt="${pet?.nombre ?? 'Mascota sin nombre'}"
              style="width:100px;height:80px;object-fit:cover;border-radius:12px;border:1px solid rgba(15,111,66,0.25);">
            <div style="display:flex;flex-direction:column;gap:4px;font-size:13px;">
              <strong style="font-size:15px;">${pet?.nombre ?? 'Mascota sin nombre'}</strong>
              <p style="margin:0;color:#4c5e50;">${detail.publication.ciudad || 'Sin datos'} · ${detail.publication.barrio || 'Sin datos'}</p>
              <button type="button" data-id="${detail.publication.id}"
                style="border:none;border-radius:999px;padding:8px 16px;background:linear-gradient(135deg,#148851,#0f6f42);color:#fff;font-weight:600;cursor:pointer;box-shadow:0 8px 16px rgba(20,136,81,0.25);">
                Ver detalle
              </button>
            </div>
          </div>
        `;
        const marker = L.marker([latitud, longitud], { icon: this.markerIcon }).bindPopup(popupHtml);
        marker.on('popupopen', (event) => {
          // Asocio el botón del popup con la navegación al detalle de la mascota.
          const button = event.popup.getElement()?.querySelector(`button[data-id="${detail.publication.id}"]`) as HTMLButtonElement | null;
          if (button) {
            button.addEventListener('click', () => this.viewRecentDetail(detail), { once: true });
          }
        });
        return marker;
      })
      .filter((marker): marker is L.Marker => !!marker);
    this.zoneMarkers = L.layerGroup(markers).addTo(this.zoneLeafletMap);
    const coords = this.getUserCoords();
    if (coords) {
      this.zoneLeafletMap.setView(coords, 15);
    } else if (markers.length) {
      const bounds = L.featureGroup(markers).getBounds().pad(0.15);
      this.zoneLeafletMap.fitBounds(bounds);
    }
  }

  // desmonto el mapa para evitar fugas de memoria cuando salgo del home
  private destroyZoneMap(): void {
    this.zoneMarkers?.remove();
    this.zoneMarkers = undefined;
    this.zoneLeafletMap?.off();
    this.zoneLeafletMap?.remove();
    this.zoneLeafletMap = undefined;
  }

  // obtengo latitud y longitud del perfil si están disponibles
  private getUserCoords(): [number, number] | null {
    const user = this.auth.currentUser();
    if (user?.latitud == null || user?.longitud == null) {
      return null;
    }
    return [user.latitud, user.longitud];
  }
}

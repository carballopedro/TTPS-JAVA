import { Component, ChangeDetectionStrategy, OnInit, ChangeDetectorRef, ElementRef, ViewChild, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NgFor, NgIf } from '@angular/common';
import { Router } from '@angular/router';
import { MascotasService, PublicationDetail } from '../../services/mascotas.service';
import { AuthService } from '../../services/auth.service';
import * as L from 'leaflet';

@Component({
  selector: 'app-mascotas-list',
  standalone: true,
  imports: [FormsModule, NgFor, NgIf],
  templateUrl: './mascotas-list.html',
  styleUrl: './mascotas-list.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MascotasListComponent implements OnInit {
  // Consumo el backend para traer las mascotas y mantengo el estado de filtros en memoria.
  protected readonly defaultImage = 'pet-placeholder.svg';

  protected filterTerm = '';
  protected filterType: 'perro' | 'gato' | 'todos' = 'todos';
  protected filterStatus: 'perdido' | 'encontrado' | 'todos' = 'todos';
  protected sortBy: 'fecha' | 'nombre' = 'fecha';
  protected sortDirection: 'asc' | 'desc' = 'desc';
  protected showMap = false; // Indica si mostramos los resultados sobre el mapa.

  protected isLoading = true;
  protected loadError = '';

  private publications: PublicationDetail[] = [];
  private map?: L.Map; // Instancia del mapa Leaflet.
  private markersLayer?: L.LayerGroup; // Agrupo los pins para actualizarlos fácilmente.
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
  private mapElement?: HTMLElement;

  protected filteredReports: PublicationDetail[] = [];

  private readonly auth = inject(AuthService);

  constructor(
    private readonly mascotasService: MascotasService,
    private readonly cdr: ChangeDetectorRef,
    private readonly router: Router,
  ) {}

  // guardo la referencia del div del mapa para inicializar Leaflet cuando haga falta
  @ViewChild('resultsMap') set resultsMap(element: ElementRef<HTMLDivElement> | undefined) {
    this.mapElement = element?.nativeElement;
    if (this.mapElement && this.showMap) {
      setTimeout(() => this.initMap(), 0); // Inicializo el mapa una vez que el DOM está listo.
    }
  }

  // al montar el componente disparo la carga inicial
  ngOnInit(): void {
    this.fetchReports();
  }

  // me permite refrescar manualmente la lista
  protected reload(): void {
    this.fetchReports();
  }

  // Filtra todas las publicaciones según texto, tipo y estado actuales.
  protected applyFilters(): void {
    const term = this.filterTerm.trim().toLowerCase();
    const filtered = this.publications.filter((item) => {
      const publication = item.publication;
      const pet = publication.mascota;
      const name = pet?.nombre?.toLowerCase() ?? '';
      const location = `${publication.ciudad ?? ''} ${publication.barrio ?? ''}`.toLowerCase();
      const petFields = [
        pet?.color ?? '',
        pet?.raza ?? '',
        pet?.tamanio ?? '',
        pet?.tipo ?? '',
        publication.descripcion ?? '',
      ]
        .join(' ')
        .toLowerCase();
      const matchesTerm =
        !term ||
        name.includes(term) ||
        location.includes(term) ||
        petFields.includes(term);

      const petType = (pet?.tipo ?? '').toLowerCase();
      const normalizedType = petType === 'gato' ? 'gato' : petType === 'perro' ? 'perro' : 'otro';
      const matchesType = this.filterType === 'todos' || normalizedType === this.filterType;

      const status = this.normalizeStatus(pet?.estado);
      const matchesStatus = this.filterStatus === 'todos' || status === this.filterStatus;

      return matchesTerm && matchesType && matchesStatus;
    });

    this.filteredReports = [...filtered].sort((a, b) => this.sortItems(a, b));
    if (this.showMap) {
      this.updateMapMarkers();
    }
  }

  // reseteo todos los filtros al estado por defecto
  protected clearFilters(): void {
    this.filterTerm = '';
    this.filterType = 'todos';
    this.filterStatus = 'todos';
    this.sortBy = 'fecha';
    this.sortDirection = 'desc';
    this.applyFilters();
  }

  // optimiza el ngFor usando el id de la publicación
  protected trackById(_: number, report: PublicationDetail): number {
    return report.publication.id;
  }

  // Navega al detalle reutilizando los datos ya cargados para evitar otro request.
  protected viewDetails(detail: PublicationDetail): void {
    const currentUserId = this.auth.userId();
    if (currentUserId != null && currentUserId === detail.creator.id) {
      // Si la publicación es propia, envío al detalle privado para mostrar el botón de eliminar y regreso al listado.
      this.router.navigate(['/publicacion-detalle', detail.publication.id], {
        state: {
          publication: detail.publication,
          backRoute: '/mascotas',
          backLabel: 'Volver al listado',
        }
      });
      return;
    }
    this.router.navigate(['/mascotas', detail.publication.id], { state: { detail, fromList: true } });
  }

  // alterno entre la vista de tarjetas y el mapa
  protected toggleResultsView(): void {
    this.showMap = !this.showMap;
    if (this.showMap) {
      setTimeout(() => this.initMap(), 0); // Preparo el mapa cuando la vista cambia.
    } else {
      this.destroyMap();
    }
  }

  // Traduce los estados técnicos del backend a etiquetas legibles.
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

  protected formatLabel(value?: string | null): string {
    if (!value) {
      return 'Sin datos';
    }
    const sanitized = value.replace(/_/g, ' ');
    const lower = sanitized.toLowerCase();
    const formatted = lower.charAt(0).toUpperCase() + lower.slice(1);
    return formatted.replace('Pequeno', 'Pequeño');
  }

  // Unifica el formato de fecha ya sea que llegue como string ISO o arreglo [año, mes, día].
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

  // Carga inicial o manual de los reportes desde el servicio.
  // consulta el listado completo y actualiza estados de carga/errores
  private fetchReports(): void {
    this.isLoading = true;
    this.loadError = '';
    this.cdr.markForCheck();

    this.mascotasService.listPublications().subscribe({
      next: (reports) => {
        this.publications = reports;
        this.applyFilters();
        this.isLoading = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.publications = [];
        this.filteredReports = [];
        this.isLoading = false;
        this.loadError = 'No pudimos cargar los reportes. Intentá nuevamente en unos minutos.';
        this.cdr.markForCheck();
      },
    });
  }

  // Simplifica los distintos estados que pueden mapearse a “perdido” o “encontrado”.
  private normalizeStatus(value?: string | null): 'perdido' | 'encontrado' {
    const normalized = value?.toLowerCase() ?? '';
    if (normalized === 'recuperado' || normalized === 'adoptado') {
      return 'encontrado';
    }
    return 'perdido';
  }

  // ordena según la preferencia actual (fecha o nombre)
  private sortItems(a: PublicationDetail, b: PublicationDetail): number {
    let compare = 0;
    if (this.sortBy === 'nombre') {
      const nameA = (a.publication.mascota?.nombre ?? '').toLowerCase();
      const nameB = (b.publication.mascota?.nombre ?? '').toLowerCase();
      compare = nameA.localeCompare(nameB);
    } else {
      const dateA = this.toTimestamp(a.publication.fecha);
      const dateB = this.toTimestamp(b.publication.fecha);
      compare = dateA - dateB;
    }
    return this.sortDirection === 'asc' ? compare : -compare;
  }

  // convierte cualquier formato de fecha en un timestamp para ordenar
  private toTimestamp(value: string | number[] | undefined): number {
    if (!value) {
      return 0;
    }
    if (Array.isArray(value) && value.length >= 3) {
      const [year, month, day] = value;
      return new Date(year, month - 1, day).getTime();
    }
    if (typeof value === 'string') {
      const parts = value.split(/[-/]/);
      if (parts.length === 3) {
        const [year, month, day] = parts;
        return new Date(Number(year), Number(month) - 1, Number(day)).getTime();
      }
      return new Date(value).getTime();
    }
    return 0;
  }

  // inicializa el mapa si aún no existe o actualiza los marcadores
  private initMap(): void {
    if (!this.mapElement || this.map) {
      this.updateMapMarkers();
      return;
    }
    this.map = L.map(this.mapElement, {
      zoomControl: true,
      scrollWheelZoom: true,
    }).setView([-34.92145, -57.95453], 12);

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      attribution: '© OpenStreetMap contributors',
    }).addTo(this.map);

    this.updateMapMarkers();
  }

  // limpia y vuelve a dibujar los pines según los filtros actuales
  private updateMapMarkers(): void {
    if (!this.map) {
      return;
    }
    this.markersLayer?.remove();
    const markers: L.Marker[] = [];
    this.filteredReports.forEach((detail) => {
      const { latitud, longitud } = detail.publication;
      if (latitud == null || longitud == null) {
        return;
      }
      const pet = detail.publication.mascota;
      const popupHtml = `
        <div class="map-popup" style="display:flex;gap:10px;align-items:center;max-width:260px;">
          <img src="${pet?.fotoUrl || this.defaultImage}" alt="${pet?.nombre ?? 'Mascota sin nombre'}"
            style="width:100px;height:80px;object-fit:cover;border-radius:12px;border:1px solid rgba(15,111,66,0.25);">
          <div style="display:flex;flex-direction:column;gap:4px;font-size:13px;">
            <strong style="font-size:15px;">${pet?.nombre ?? 'Mascota sin nombre'}</strong>
            <p style="margin:0;color:#4c5e50;">${detail.publication.ciudad || 'Sin datos'} · ${detail.publication.barrio || 'Sin datos'}</p>
            <button data-id="${detail.publication.id}"
              style="border:none;border-radius:999px;padding:8px 16px;background:linear-gradient(135deg,#148851,#0f6f42);color:#fff;font-weight:600;cursor:pointer;box-shadow:0 8px 16px rgba(20,136,81,0.25);">
              Ver detalle
            </button>
          </div>
        </div>
      `;
      const marker = L.marker([latitud, longitud], { icon: this.markerIcon }).bindPopup(popupHtml);
      marker.on('popupopen', (event) => {
        const button = event.popup.getElement()?.querySelector('button[data-id]') as HTMLButtonElement | null;
        if (button) {
          button.addEventListener('click', () => this.viewDetails(detail));
        }
      });
      markers.push(marker);
    });
    this.markersLayer = L.layerGroup(markers).addTo(this.map);
    if (markers.length) {
      const group = L.featureGroup(markers);
      this.map.fitBounds(group.getBounds().pad(0.15));
    }
  }

  // remueve listeners y capas del mapa cuando oculto la vista
  private destroyMap(): void {
    this.markersLayer?.remove();
    this.markersLayer = undefined;
    this.map?.off();
    this.map?.remove();
    this.map = undefined;
  }
}

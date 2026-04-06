import { Component, ElementRef, OnDestroy, OnInit, ViewChild, inject } from '@angular/core';
import { NgIf, NgFor } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import * as L from 'leaflet';
import { AuthService, UpdateProfilePayload } from '../../services/auth.service';
import { GeocodingService } from '../../services/geocoding.service';

@Component({
  selector: 'app-perfil',
  standalone: true,
  imports: [NgIf, NgFor, ReactiveFormsModule],
  templateUrl: './perfil.html',
  styleUrl: './perfil.css',
})
export class PerfilComponent implements OnInit, OnDestroy {
  private readonly auth = inject(AuthService);
  private readonly fb = inject(FormBuilder);
  private readonly geocoding = inject(GeocodingService);

  // Me suscribo al signal del servicio para mostrar el perfil sin pegar nuevas llamadas.
  protected readonly user = this.auth.currentUser;
  protected isEditing = false; // determina si el formulario está editable
  protected isSaving = false; // flag de carga al guardar cambios
  protected saveError?: string;
  protected isGeocoding = false; // flag de geocodificación
  protected geoError?: string;
  protected isPasswordVisible = false;

  // Traduce el nombre del badge a texto legible acompañado de su emoji correspondiente.
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
    return badge ?? 'Insignia';
  }

  // Formulario reactivo con los campos que el usuario puede actualizar.
  protected readonly profileForm = this.fb.nonNullable.group({
    nombre: ['', Validators.required],
    apellido: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
    password: [''],
    telefono: ['', Validators.required],
    barrio: ['', Validators.required],
    ciudad: ['', Validators.required],
    instagram: [''],
    sitioWeb: [''],
  });

  private map?: L.Map;
  private marker?: L.Marker;
  private mapElement?: HTMLElement;
  private viewMap?: L.Map;
  private viewMarker?: L.Marker;
  private viewMapElement?: HTMLElement;
  private selectedLat?: number;
  private selectedLng?: number;
  private readonly defaultCenter: [number, number] = [-34.92145, -57.95453];

  private readonly markerIconUrl =
    'data:image/svg+xml;utf8,<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"30\" height=\"40\" viewBox=\"0 0 30 40\"><path d=\"M15 0C8 0 2.5 5.5 2.5 12.5C2.5 22 15 40 15 40C15 40 27.5 22 27.5 12.5C27.5 5.5 22 0 15 0Z\" fill=\"%23148851\" stroke=\"%230f6f42\" stroke-width=\"2\"/><circle cx=\"15\" cy=\"13\" r=\"4\" fill=\"white\"/></svg>';
  private readonly markerIcon = L.icon({
    iconRetinaUrl: this.markerIconUrl,
    iconUrl: this.markerIconUrl,
    iconSize: [30, 40],
    iconAnchor: [15, 40],
    popupAnchor: [0, -36],
    tooltipAnchor: [0, -36],
    shadowUrl: undefined,
  });

  @ViewChild('profileMap') set profileMap(element: ElementRef<HTMLDivElement> | undefined) {
    // Guardo la referencia al contenedor y creo el mapa cuando aparece en el DOM.
    this.mapElement = element?.nativeElement;
    if (this.mapElement && this.isEditing) {
      this.initMap();
    }
  }

  @ViewChild('profileViewMap') set profileViewMap(element: ElementRef<HTMLDivElement> | undefined) {
    this.viewMapElement = element?.nativeElement;
    if (this.viewMapElement && !this.isEditing) {
      setTimeout(() => this.initViewMap(), 0);
    }
  }

  // cuando se monta el perfil refresco datos para mostrar badges y puntos
  ngOnInit(): void {
    this.auth.ensureProfileLoaded();
    this.auth.refreshProfile(); // fuerzo actualización inmediata para reflejar puntos/novedades sin recargar
  }

  // libero los mapas para evitar fugas al salir de la vista
  ngOnDestroy(): void {
    this.destroyMap();
    this.destroyViewMap();
  }

  // habilita la edición tomando los valores actuales y mostrando el mapa interactivo
  protected enableEditing(): void {
    const profile = this.user();
    if (!profile) {
      return;
    }
     this.destroyViewMap();
    // Precargo el formulario con los valores actuales.
    this.profileForm.reset({
      nombre: profile.nombre ?? '',
      apellido: profile.apellido ?? '',
      email: profile.email ?? '',
      password: '',
      telefono: profile.telefono ?? '',
      barrio: profile.barrio ?? '',
      ciudad: profile.ciudad ?? '',
      instagram: profile.instagram ?? '',
      sitioWeb: profile.sitioWeb ?? '',
    });
    this.selectedLat = profile.latitud ?? undefined;
    this.selectedLng = profile.longitud ?? undefined;
    this.saveError = undefined;
    this.geoError = undefined;
    this.isEditing = true;
    // Espero al render para inicializar el mapa sin errores.
    setTimeout(() => this.initMap(), 0);
  }

  // vuelve al modo lectura descartando cambios pendientes
  protected cancelEditing(): void {
    this.isEditing = false;
    this.isSaving = false;
    this.profileForm.reset();
    this.saveError = undefined;
    this.geoError = undefined;
    this.selectedLat = undefined;
    this.selectedLng = undefined;
    this.destroyMap();
  }

  // muestra u oculta el campo contraseña para facilitar la edición
  protected togglePasswordVisibility(): void {
    this.isPasswordVisible = !this.isPasswordVisible;
  }

  // valida el formulario y envía los cambios al backend
  protected saveProfile(): void {
    const profile = this.user();
    if (!profile) {
      return;
    }
    if (this.profileForm.invalid) {
      this.profileForm.markAllAsTouched();
      return;
    }
    this.isSaving = true;
    this.saveError = undefined;

    const formValue = this.profileForm.getRawValue();
    // Normalizo los campos opcionales para que si quedan vacíos se envíe un string vacío y el backend los borre.
    const normalizedInstagram = formValue.instagram?.trim() ?? '';
    const normalizedWebsite = formValue.sitioWeb?.trim() ?? '';

    const payload: UpdateProfilePayload = {
      nombre: formValue.nombre,
      apellido: formValue.apellido,
      email: formValue.email,
      telefono: formValue.telefono,
      barrio: formValue.barrio,
      ciudad: formValue.ciudad,
      instagram: normalizedInstagram,
      sitioWeb: normalizedWebsite,
    };
    if (this.selectedLat != null && this.selectedLng != null) {
      payload.latitud = this.selectedLat;
      payload.longitud = this.selectedLng;
    }

    if (formValue.password?.trim()) {
      payload.password = formValue.password.trim();
    }

    this.auth.updateProfile(profile.id, payload).subscribe({
      next: () => {
        this.isSaving = false;
        this.isEditing = false;
        this.destroyMap();
        this.auth.refreshProfile(); // vuelvo a pedir el perfil completo para reflejar badges y puntos actuales
        setTimeout(() => this.initViewMap(), 0);
      },
      error: (error) => {
        this.isSaving = false;
        this.saveError =
          typeof error?.error === 'string'
            ? error.error
            : 'No pudimos guardar los cambios. Intentá nuevamente.';
      },
    });
  }

  // inicializa el mapa de edición y configura el click para elegir coordenadas
  private initMap(): void {
    if (!this.mapElement || this.map) {
      return;
    }
    const profileCoords = this.getUserCoordinates();
    const initialCenter = profileCoords ?? this.defaultCenter;
    const initialZoom = profileCoords ? 15 : 13;

    this.map = L.map(this.mapElement, {
      zoomControl: true,
      scrollWheelZoom: true,
    }).setView(initialCenter, initialZoom);

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      attribution: '© OpenStreetMap contributors',
    }).addTo(this.map);

    if (profileCoords) {
      this.updateMarker(profileCoords[0], profileCoords[1]);
    }

    this.map.on('click', (event: L.LeafletMouseEvent) => {
      const { lat, lng } = event.latlng;
      this.selectedLat = lat;
      this.selectedLng = lng;
      this.updateMarker(lat, lng);
      this.lookupLocation(lat, lng);
    });
  }

  // remueve listeners del mapa editable
  private destroyMap(): void {
    this.map?.off();
    this.map?.remove();
    this.map = undefined;
    this.marker = undefined;
  }

  // arma el mapa de solo lectura con la ubicación guardada
  private initViewMap(): void {
    if (!this.viewMapElement || this.viewMap || this.isEditing) {
      return;
    }
    const coords = this.getUserCoordinates();
    if (!coords) {
      return;
    }
    this.viewMap = L.map(this.viewMapElement, {
      zoomControl: false,
      dragging: false,
      scrollWheelZoom: false,
      doubleClickZoom: false,
      boxZoom: false,
      keyboard: false,
      touchZoom: false,
    }).setView(coords, 15);

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      attribution: '© OpenStreetMap contributors',
    }).addTo(this.viewMap);

    this.viewMarker = L.marker(coords, { icon: this.markerIcon }).addTo(this.viewMap);
  }

  // limpia el mapa de vista cuando cambio de modo
  private destroyViewMap(): void {
    this.viewMap?.off();
    this.viewMap?.remove();
    this.viewMap = undefined;
    this.viewMarker = undefined;
    this.viewMapElement = undefined;
  }

  // mueve o crea el marcador según la coordenada seleccionada
  private updateMarker(lat: number, lng: number): void {
    if (!this.map) {
      return;
    }
    if (!this.marker) {
      this.marker = L.marker([lat, lng], { icon: this.markerIcon }).addTo(this.map);
    } else {
      this.marker.setLatLng([lat, lng]);
    }
    this.map.setView([lat, lng]);
  }

  // consulta el servicio de geocodificación y completa barrio/ciudad
  private lookupLocation(lat: number, lng: number): void {
    this.isGeocoding = true;
    this.geoError = undefined;

    this.geocoding.lookup(lat, lng).subscribe({
      next: (location) => {
        // Actualizo barrio y ciudad usando la respuesta del servicio de geocodificación.
        this.profileForm.patchValue({
          barrio: location.neighborhood,
          ciudad: location.city,
        });
        this.isGeocoding = false;
      },
      error: () => {
        this.geoError = 'No pudimos obtener la ubicación. Intentá nuevamente.';
        this.isGeocoding = false;
      },
    });
  }

  // devuelve las coordenadas guardadas del perfil si existen
  private getUserCoordinates(): [number, number] | null {
    const profile = this.user();
    if (profile?.latitud == null || profile?.longitud == null) {
      return null;
    }
    return [profile.latitud, profile.longitud];
  }
}

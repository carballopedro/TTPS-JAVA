import { Component, AfterViewInit, OnDestroy, ViewChild, ElementRef, inject } from '@angular/core';
import { CommonModule, NgIf } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import * as L from 'leaflet';
import { GeocodingService } from '../../services/geocoding.service';
import { AuthService } from '../../services/auth.service';


@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, NgIf],
  templateUrl: './register.html',
  styleUrl: './register.css',
})
export class RegisterComponent implements AfterViewInit, OnDestroy {
  private readonly fb = inject(FormBuilder);

  // Servicio de geocodificación para buscar ciudad y barrio
  private readonly geocoding = inject(GeocodingService);

  // Uso este servicio para registrar el nuevo usuario
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  protected isSubmitting = false;
  protected serverError?: string;
  @ViewChild('registerFormElement') private readonly formElement?: ElementRef<HTMLFormElement>; // referencia al formulario para poder disparar reportValidity


  private map?: L.Map;
  private marker?: L.Marker;
  // Icono personalizado que uso para marcar la posición seleccionada en el mapa
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

  protected isPasswordVisible = false; // uso este flag para mostrar el icono de ver/ocultar
  protected isGeocoding = false;
  protected geoError?: string;

  // Formulario reactivo con los datos que tiene que completar la persona
  protected readonly registerForm = this.fb.group({
    firstName: this.fb.nonNullable.control('', Validators.required),
    lastName: this.fb.nonNullable.control('', Validators.required),
    email: this.fb.nonNullable.control('', [Validators.required, Validators.email]),
    password: this.fb.nonNullable.control('', Validators.required),
    city: this.fb.nonNullable.control('', Validators.required),
    neighborhood: this.fb.nonNullable.control('', Validators.required),
    phone: this.fb.nonNullable.control('', Validators.required),
    latitude: this.fb.control<number | null>(null, Validators.required),
    longitude: this.fb.control<number | null>(null, Validators.required),
  });

  // cuando el template ya está renderizado armo el mapa interactivo
  ngAfterViewInit(): void {
    // Creo el mapa apenas el componente se pinta
    this.initMap();
  }

  // limpio recursos del mapa al abandonar la pantalla
  ngOnDestroy(): void {
    // Limpio los listeners del mapa para evitar fugas de memoria
    this.map?.off();
    this.map?.remove();
  }

  // alterna la visibilidad del campo contraseña
  protected togglePasswordVisibility() {
    // Alterno entre mostrar u ocultar la contraseña
    this.isPasswordVisible = !this.isPasswordVisible;
  }

  // valida el formulario y registra al usuario si todo está completo
  protected onSubmit(): void {
    // Si hay errores muestro todos los campos tocados y corto
    if (this.registerForm.invalid) {
      this.registerForm.markAllAsTouched();
      this.formElement?.nativeElement.reportValidity(); // fuerzo el mensaje nativo para explicar qué campo falta
      return;
    }
    this.isSubmitting = true;
    this.serverError = undefined;

    const { firstName, lastName, email, password, phone, neighborhood, city, latitude, longitude } = this.registerForm.getRawValue();
    if (latitude == null || longitude == null) {
      this.isSubmitting = false;
      this.serverError = 'Seleccioná un punto en el mapa antes de continuar.';
      return;
    }
    const payload = {
      nombre: firstName,
      apellido: lastName,
      email,
      password,
      telefono: phone,
      barrio: neighborhood,
      ciudad: city,
      latitud: latitude,
      longitud: longitude,
    };

    this.auth.register(payload).subscribe({
      next: () => {
        this.isSubmitting = false;
        this.router.navigateByUrl('/login');
      },
      error: (error) => {
        this.isSubmitting = false;
        this.serverError =
          typeof error?.error === 'string'
            ? error.error
            : error?.error?.message ?? 'No pudimos registrarte. Intentá de nuevo.';
      },
    });
  }

  // inicializa Leaflet y configura los eventos de click para elegir ubicación
  private initMap(): void {
    // Configuro el mapa centrado en La Plata con controles básicos
    this.map = L.map('register-map', {
      zoomControl: true,
      scrollWheelZoom: true,
    }).setView([-34.92145, -57.95453], 13);

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      attribution: '© OpenStreetMap contributors',
    }).addTo(this.map);

    this.map.on('click', (event: L.LeafletMouseEvent) => {
      const { lat, lng } = event.latlng;
      this.registerForm.patchValue({
        latitude: lat,
        longitude: lng,
      });
      this.updateMarker(lat, lng);
      this.lookupLocation(lat, lng);
    });
  }

  // crea o mueve el pin usando las coordenadas seleccionadas
  private updateMarker(lat: number, lng: number): void {
    // Creo el marcador si no existe o sólo actualizo su posición
    if (!this.map) {
      return;
    }
    if (!this.marker) {
      this.marker = L.marker([lat, lng], { icon: this.markerIcon }).addTo(this.map);
    } else {
      this.marker.setLatLng([lat, lng]);
      this.marker.setIcon(this.markerIcon);
    }
    this.map.setView([lat, lng]);
  }

  // consulta el servicio de geocodificación para completar ciudad y barrio
  private lookupLocation(lat: number, lng: number): void {
    // Disparo la búsqueda inversa y muestro un loader y errores si corresponde
    this.isGeocoding = true;
    this.geoError = undefined;

    this.geocoding.lookup(lat, lng).subscribe({
      next: (location) => {
        // Actualizo el formulario con la ciudad y barrio detectados
        this.registerForm.patchValue({
          city: location.city,
          neighborhood: location.neighborhood,
        });
        this.isGeocoding = false;
      },
      error: () => {
        // Aviso que hubo un problema obteniendo datos de geocodificación
        this.geoError = 'No pudimos obtener la ubicación. Intentá nuevamente.';
        this.isGeocoding = false;
      },
    });
  }
}

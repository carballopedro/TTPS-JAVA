import { AfterViewInit, ChangeDetectorRef, Component, ElementRef, NgZone, OnDestroy, OnInit, ViewChild, inject } from '@angular/core';
import { CommonModule, NgIf } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import * as L from 'leaflet';
import { GeocodingService } from '../../services/geocoding.service';
import { AuthService } from '../../services/auth.service';
import { PublicacionesService } from '../../services/publicaciones.service';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-publicacion',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, NgIf],
  templateUrl: './publicacion.html',
  styleUrl: './publicacion.css',
})
export class PublicacionComponent implements AfterViewInit, OnDestroy, OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly geocoding = inject(GeocodingService);
  private readonly publicaciones = inject(PublicacionesService);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly zone = inject(NgZone);
  @ViewChild('publicationFormElement') private readonly formElement?: ElementRef<HTMLFormElement>; // referencia al form para disparar reportValidity

  // variables para el manejo del msj de confirmación de mascota publicada correctamente
  protected successPromptVisible = false;
  protected successMessage?: string;
  private successRedirect = '/mis-publicaciones'; // destino por defecto tras confirmar el mensaje


  protected geoError?: string;
  protected photoName?: string;
  protected readonly today = new Date().toISOString().split('T')[0]; // límite máximo para el input de fecha

  private map?: L.Map;
  private marker?: L.Marker;
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

  protected readonly petTypes = ['PERRO', 'GATO', 'OTRO'] as const;
  protected readonly petSizes = ['PEQUENO', 'MEDIANO', 'GRANDE'] as const;
  protected readonly petStatuses = ['PERDIDO_PROPIO', 'PERDIDO_AJENO'] as const;
  protected breedOptions: readonly string[] = []; // lista de razas disponibles para el tipo seleccionado
  protected isBreedEnabled = false; // determina si el select de raza debe estar activo

  // Tabla de razas posibles dependiendo del tipo elegido para habilitar solo las válidas.
  private readonly breedMap: Record<(typeof this.petTypes)[number], readonly string[]> = {
    PERRO: ['LABRADOR', 'BULLDOG', 'OVEJERO', 'GOLDEN_RETRIEVER', 'CANICHE', 'OTRO'],
    GATO: ['SIAMES', 'PERSA', 'MAINE_COON', 'BENGALI', 'ANGORA', 'OTRO'],
    OTRO: ['OTRO'],
  };
  private breedSubscription?: Subscription;

  // Formulario reactivo con todos los campos que el usuario completa para crear una publicación.
  protected readonly publicationForm = this.fb.nonNullable.group({
    date: ['', Validators.required],
    description: ['', Validators.required],
    city: ['', Validators.required],
    neighborhood: ['', Validators.required],
    petName: [''],
    color: ['', Validators.required],
    type: ['', Validators.required],
    size: ['', Validators.required],
    status: ['', Validators.required],
    breed: ['', Validators.required],
    photo: [null as File | null, Validators.required],
  });

  // configuro la lógica de razas apenas se inicializa el componente
  ngOnInit(): void {
    this.setupBreedControl(); // configuro la dependencia entre tipo y raza ni bien se inicializa el componente
  }

  // preparo el mapa de selección cuando el template ya está montado
  ngAfterViewInit(): void {
    this.initMap();
  }

  // limpio mapas y suscripciones cuando salgo del formulario
  ngOnDestroy(): void {
    this.map?.off();
    this.map?.remove();
    this.breedSubscription?.unsubscribe();
  }

  // valida el formulario, arma el payload y lo envía al backend
  protected onSubmit(): void {
    if (this.publicationForm.invalid) {
      this.publicationForm.markAllAsTouched();
      this.formElement?.nativeElement.reportValidity(); // muestro el tooltip nativo indicando qué falta completar
      return;
    }
    const user = this.auth.currentUser();
    if (!user) {
      console.warn('Usuario no autenticado');
      return;
    }
    const form = this.publicationForm.getRawValue();
    // Armo el cuerpo lógico de la publicación con todos los datos del formulario + coordenadas del mapa.
    const publicationData = {
      fecha: form.date,
      descripcion: form.description,
      latitud: this.marker?.getLatLng().lat ?? null,
      longitud: this.marker?.getLatLng().lng ?? null,
      barrio: form.neighborhood,
      ciudad: form.city,
      mascota: {
        nombre: form.petName || 'Sin nombre',
        color: form.color,
        tipo: form.type,
        tamanio: form.size,
        estado: form.status,
        raza: form.breed,
      },
      creador: {
        id: user.id,
      },
    };
    const payload = new FormData();
    // Envío los datos JSON como un blob dentro del multipart, tal como espera el backend.
    payload.append('publicacion', new Blob([JSON.stringify(publicationData)], { type: 'application/json' }));
    if (form.photo) {
      // Si el usuario eligió una foto, la adjunto tal cual para que viaje como archivo binario.
      payload.append('fotos', form.photo);
    }
    this.publicaciones.createPublication(payload).subscribe({
      next: () => {
        // Fuerzo la ejecución en la zona de Angular para que se detecten los cambios sin interacción extra.
        this.zone.run(() => {
          console.log('Publicación creada');
          this.successMessage =
            'La mascota perdida se ha publicado correctamente! Gracias por tu ayuda, sumaste 10 puntos!';
          this.successRedirect = '/mis-publicaciones';
          this.successPromptVisible = true;
          this.cdr.markForCheck();
        });
      },
      error: () => console.error('Error al crear publicación'),
    });
  }

  // Manejo el cambio del input de archivo para guardar la foto en el formulario reactivo.
  // guarda el archivo elegido y actualiza la vista previa del nombre
  protected onPhotoSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files?.length) {
      this.publicationForm.patchValue({ photo: null });
      this.photoName = undefined;
      return;
    }
    const file = input.files[0];
    // Guardo la foto en el form reactivo y muestro el nombre del archivo para confirmar la selección.
    this.publicationForm.patchValue({ photo: file });
    this.photoName = file.name;
  }

  // inicializa el mapa clickeable para elegir la ubicación de la mascota
  private initMap(): void {
    this.map = L.map('publication-map', {
      zoomControl: true,
      scrollWheelZoom: true,
    }).setView([-34.92145, -57.95453], 13);

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      attribution: '© OpenStreetMap contributors',
    }).addTo(this.map);

    this.map.on('click', (event: L.LeafletMouseEvent) => {
      const { lat, lng } = event.latlng;
      this.updateMarker(lat, lng);
      this.lookupLocation(lat, lng);
    });
  }

  // crea o mueve el marcador en la coordenada seleccionada
  private updateMarker(lat: number, lng: number): void {
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

  // busca barrio y ciudad según la coordenada elegida y los vuelca al formulario
  private lookupLocation(lat: number, lng: number): void {
    this.geoError = undefined;

    this.geocoding.lookup(lat, lng).subscribe({
      next: (location) => {
        this.publicationForm.patchValue({
          city: location.city,
          neighborhood: location.neighborhood,
        });
      },
      error: () => {
        this.geoError = 'No pudimos obtener los datos del barrio. Intentá nuevamente.';
      },
    });
  }

  // normaliza textos reemplazando guiones bajos por espacios
  protected formatLabel(value: string): string {
    return value.replace(/_/g, ' ');
  }

  // sincroniza el combo de raza con el tipo seleccionado para evitar datos inválidos
  private setupBreedControl(): void {
    const typeControl = this.publicationForm.get('type');
    const breedControl = this.publicationForm.get('breed');
    if (!typeControl || !breedControl) {
      return;
    }
    breedControl.disable(); // inicio con el selector deshabilitado hasta que haya tipo definido
    this.breedSubscription = typeControl.valueChanges.subscribe((type) => {
      const normalized = (type ?? '') as (typeof this.petTypes)[number] | '';
      const options = normalized ? this.breedMap[normalized] ?? [] : [];
      this.breedOptions = options;
      if (!normalized || options.length === 0) {
        this.isBreedEnabled = false;
        breedControl.disable();
        breedControl.setValue('');
        return;
      }
      this.isBreedEnabled = true;
      breedControl.enable();
      if (!options.includes((breedControl.value as string) ?? '')) {
        breedControl.setValue('');
      }
    });
  }

  // manejo confirmación del usuario cuando publica mascota correctamente
  // al cerrar el modal de éxito redirijo al listado correspondiente
  protected confirmSuccess(): void {
  if (!this.successPromptVisible) {
    return;
  }
  this.successPromptVisible = false;
  const target = this.successRedirect || '/home';
  this.successMessage = undefined;
  this.router.navigate([target]);
}

}

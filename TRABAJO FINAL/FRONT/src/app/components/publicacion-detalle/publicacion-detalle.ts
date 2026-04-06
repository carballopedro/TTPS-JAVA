import { Component, ChangeDetectionStrategy, ElementRef, OnDestroy, ViewChild, ChangeDetectorRef, effect, inject, signal } from '@angular/core';
import { NgIf } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import * as L from 'leaflet';
import { PublicacionesService, UserPublication } from '../../services/publicaciones.service';
import { AuthService } from '../../services/auth.service';
import { Subscription } from 'rxjs';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

@Component({
  selector: 'app-publicacion-detalle',
  standalone: true,
  imports: [NgIf, RouterLink, ReactiveFormsModule],
  templateUrl: './publicacion-detalle.html',
  styleUrl: './publicacion-detalle.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PublicacionDetalleComponent implements OnDestroy {
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly publicaciones = inject(PublicacionesService);
  private readonly auth = inject(AuthService);
  private readonly fb = inject(FormBuilder);
  private readonly cdr = inject(ChangeDetectorRef);
  private ownershipSub?: Subscription;
  private publicationSub?: Subscription;

  protected publication?: UserPublication;
  protected readonly canDelete = signal(false);
  private readonly syncCanDelete = effect(
    () => {
      // Re-evalúo la propiedad cada vez que cambia el ID del usuario logueado.
      this.auth.userId();
      this.evaluateOwnership();
    },
    { allowSignalWrites: true }
  );
  protected readonly defaultImage = 'pet-placeholder.svg'; // imagen por defecto para detalle
  protected backRoute = '/mis-publicaciones'; // ruta de regreso por defecto
  protected backLabel = 'Volver al listado'; // texto asociado al link de regreso
  protected isEditing = false; // indica si el formulario de edición está visible
  protected isSaving = false; // flag de carga al guardar la edición
  protected saveError?: string; // mensaje de error cuando la actualización falla
  // Formulario reactivo con los únicos campos permitidos en la edición.
  protected readonly editForm = this.fb.group({
    description: ['', Validators.required],
    petName: ['', Validators.required],
  });
  protected deletePromptVisible = false;
  protected isDeleting = false;
  protected deleteError?: string;
  protected adoptionPromptVisible = false; // modal para confirmar adopción
  protected recoveryPromptVisible = false; // modal para confirmar recuperación
  protected isUpdatingState = false; // flag de carga al cambiar estado
  protected stateChangeError?: string;
  protected stateChangeSuccessMessage?: string; // mensaje mostrado tras un cambio exitoso
  private stateChangeSuccessRoute?: string; // ruta a la que debo volver tras confirmar
  protected readonly formatLabel = (value?: string | null) => {
    if (!value) {
      return 'Sin datos';
    }
    const lower = value.toLowerCase();
    const formatted = lower.charAt(0).toUpperCase() + lower.slice(1);
    return formatted.replace('Pequeno', 'Pequeño');
  };
  protected readonly formatPetStatus = (value?: string | null) => {
    if (!value) {
      return undefined;
    }
    const normalized = value.toLowerCase();
    if (normalized === 'perdido_propio' || normalized === 'perdido_ajeno') {
      return 'Perdido';
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

  // leo el state de navegación o, si no existe, vuelvo a pedir la publicación por ID
  constructor() {
    // Intento obtener la publicación desde el estado de navegación para evitar otro request.
    const navigationState = this.router.getCurrentNavigation()?.extras.state as {
      publication?: UserPublication;
      backRoute?: string;
      backLabel?: string;
    } | undefined;
    this.publication = navigationState?.publication ?? (history.state?.publication as UserPublication | undefined);
    // Leo la ruta de retorno para respetar el origen desde el que se abrió el detalle.
    const backRoute = navigationState?.backRoute ?? (history.state?.backRoute as string | undefined);
    const backLabel = navigationState?.backLabel ?? (history.state?.backLabel as string | undefined);
    if (backRoute) {
      this.backRoute = backRoute;
    }
    if (backLabel) {
      this.backLabel = backLabel;
    }
    if (this.publication) {
      this.evaluateOwnership();
      return;
    }
    // Si vengo desde una URL directa, uso el ID de la ruta para volver a pedir el detalle.
    const routeId = this.route.snapshot.paramMap.get('id');
    const idParam = routeId ? Number(routeId) : NaN;
    if (!routeId || Number.isNaN(idParam)) {
      this.router.navigate(['/mis-publicaciones']);
      return;
    }
    this.loadPublicationById(idParam);

  }

  // al salir destruyo mapa y suscripciones
  ngOnDestroy(): void {
    this.destroyMap();
    this.ownershipSub?.unsubscribe();
    this.publicationSub?.unsubscribe();
  }

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

  private initMap(): void {
    if (!this.mapElement || this.map || !this.publication) {
      return;
    }
    const { latitud, longitud } = this.publication;
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

  private destroyMap(): void {
    this.map?.off();
    this.map?.remove();
    this.map = undefined;
    this.marker = undefined;
  }

  // Descargo la publicación por ID cuando no existe en el state para mantener botón y datos consistentes.
  private loadPublicationById(publicationId: number): void {
    this.publicationSub?.unsubscribe();
    this.publicationSub = this.publicaciones.getById(publicationId).subscribe({
      next: (publication) => {
        this.publication = publication;
        this.evaluateOwnership();
      },
      error: () => this.router.navigate(['/mis-publicaciones']),
    });
  }

  // Prepara el formulario con los datos actuales y habilita el modo edición.
  protected startEditing(): void {
    if (!this.publication) {
      return;
    }
    const pet = this.publication.mascota ?? {};
    this.editForm.reset({
      description: this.publication.descripcion ?? '',
      petName: pet.nombre ?? '',
    });
    this.saveError = undefined;
    this.isEditing = true;
  }

  // Cancela la edición y limpia el formulario sin persistir cambios.
  protected cancelEditing(): void {
    this.isEditing = false;
    this.isSaving = false;
    this.saveError = undefined;
    this.editForm.reset();
  }

  // Determina si un control requerido está inválido para mostrar el mensaje de error.
  protected isFieldInvalid(controlName: string): boolean {
    const control = this.editForm.get(controlName);
    return !!control && control.invalid && (control.dirty || control.touched);
  }

  // Muestra el botón "Recuperar" cuando el usuario es dueño y la mascota sigue marcada como perdido propio.
  protected shouldShowRecoverButton(): boolean {
    const state = this.normalizePetState(this.publication?.mascota?.estado);
    const userId = this.auth.userId();
    if (userId == null || !this.publication) {
      return false;
    }
    return this.isOwner(userId, this.publication) && state === 'perdido_propio';
  }

  // Habilita el botón "Adoptar" únicamente para usuarios logueados cuando el estado es perdido ajeno.
  protected shouldShowAdoptButton(): boolean {
    const state = this.normalizePetState(this.publication?.mascota?.estado);
    const userId = this.auth.userId();
    if (userId == null || !this.publication) {
      return false;
    }
    return state === 'perdido_ajeno';
  }

  // Abro la confirmación para adoptar una mascota ajena.
  protected openAdoptPrompt(): void {
    this.stateChangeError = undefined;
    this.adoptionPromptVisible = true;
  }

  // Abro la confirmación para marcar como recuperada la publicación propia.
  protected openRecoverPrompt(): void {
    this.stateChangeError = undefined;
    this.recoveryPromptVisible = true;
  }

  protected cancelStatePrompt(): void {
    this.adoptionPromptVisible = false;
    this.recoveryPromptVisible = false;
  }

  // Llamo a la API con la acción Adoptar.
  protected confirmAdopt(): void {
    this.updateState('Adoptar');
  }

  // Llamo a la API con la acción Recuperar.
  protected confirmRecover(): void {
    this.updateState('Recuperar');
  }

  // Envía los cambios al backend y actualiza el detalle en pantalla.
  protected submitEdit(): void {
    if (!this.publication) {
      return;
    }
    if (this.editForm.invalid) {
      this.editForm.markAllAsTouched();
      return;
    }
    this.isSaving = true;
    this.saveError = undefined;
    const form = this.editForm.getRawValue();
    const descripcion = form.description?.trim() || this.publication.descripcion || '';
    const petName = form.petName?.trim() || this.publication.mascota?.nombre || '';
    const payload = {
      descripcion,
      mascota: { nombre: petName },
    };
    const publicationId = this.publication.id;
    this.publicaciones.updatePublication(publicationId, payload).subscribe({
      next: () => {
        this.isSaving = false;
        this.isEditing = false;
        this.editForm.reset();
        this.publication = {
          ...(this.publication as UserPublication),
          descripcion,
          mascota: { ...(this.publication?.mascota ?? {}), nombre: petName },
        };
      },
      error: () => {
        this.isSaving = false;
        this.saveError = 'No pudimos guardar los cambios. Intentá nuevamente.';
      },
    });
  }

  // Determina si el usuario logueado es dueño de la publicación (incluso tras un refresh).
  private evaluateOwnership(): void {
    const userId = this.auth.userId();
    const publication = this.publication;

    if (userId == null || !publication?.id) {
      this.canDelete.set(false);
      return;
    }

    const ownerId = (publication as any)?.creador?.id ?? publication.creador_id;
    if (ownerId != null) {
      this.canDelete.set(ownerId === userId);
      return;
    }

    // Si no vino el dato del creador en la navegación, consulto mis publicaciones y busco el ID actual.
    this.ownershipSub?.unsubscribe();
    this.ownershipSub = this.publicaciones.getByUser(userId).subscribe({
      next: (items) => {
        const owns = items.some((item) => item.id === publication.id);
        this.canDelete.set(owns);
      },
      error: () => this.canDelete.set(false),
    });
  }

  // Convierte distintos formatos de fecha en el valor esperado por un input tipo date.
  private toDateInputValue(value: string | number[] | undefined): string {
    if (!value) {
      return '';
    }
    if (Array.isArray(value) && value.length >= 3) {
      const [year, month, day] = value;
      return `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
    }
    if (typeof value === 'string') {
      const parts = value.split(/[-/]/);
      if (parts.length === 3) {
        const [a, b, c] = parts;
        // Detecto si viene como YYYY-MM-DD o DD/MM/YYYY para acomodarlo al input.
        if (a.length === 4) {
          return `${a}-${b.padStart(2, '0')}-${c.padStart(2, '0')}`;
        }
        return `${c}-${b.padStart(2, '0')}-${a.padStart(2, '0')}`;
      }
      const parsed = new Date(value);
      if (!Number.isNaN(parsed.getTime())) {
        return parsed.toISOString().slice(0, 10);
      }
    }
    return '';
  }

  // Centralizo el chequeo de propiedad para reutilizarlo en botones condicionales.
  private isOwner(userId: number | null, publication: UserPublication | undefined = this.publication): boolean {
    if (userId == null || !publication) {
      return false;
    }
    const ownerId = (publication as any)?.creador?.id ?? publication.creador_id;
    return ownerId === userId;
  }

  // Normalizo el estado de la mascota para comparaciones simples.
  private normalizePetState(value?: string | null): string | undefined {
    return value?.toLowerCase();
  }

  // Reutilizo este método para invocar el endpoint de estado con confirmaciones previas.
  private updateState(accion: 'Adoptar' | 'Recuperar'): void {
    if (!this.publication || this.isUpdatingState) {
      return;
    }
    this.isUpdatingState = true;
    this.stateChangeError = undefined;
    this.publicaciones.changePublicationState(this.publication.id, accion).subscribe({
      next: () => {
        this.isUpdatingState = false;
        this.adoptionPromptVisible = false;
        this.recoveryPromptVisible = false;
        const newState = accion === 'Adoptar' ? 'adoptado' : 'recuperado';
        this.publication = {
          ...(this.publication as UserPublication),
          mascota: { ...(this.publication?.mascota ?? {}), estado: newState },
        };
        // Muestro mensaje de éxito antes de volver al listado.
        this.stateChangeSuccessMessage =
          accion === 'Adoptar'
            ? 'La mascota ha sido adoptada, sumaste 10 puntos.'
            : 'La mascota ha sido recuperada.';
        this.stateChangeSuccessRoute = '/mis-publicaciones';
        this.cdr.markForCheck(); // fuerzas que el overlay se actualice bajo OnPush
      },
      error: () => {
        this.isUpdatingState = false;
        this.stateChangeError = 'No pudimos actualizar el estado. Intentá nuevamente.';
        this.cdr.markForCheck();
      },
    });
  }

  // Confirmo el mensaje de éxito y redirijo donde corresponda.
  protected confirmStateSuccess(): void {
    if (!this.stateChangeSuccessMessage) {
      return;
    }
    const target = this.stateChangeSuccessRoute ?? '/mis-publicaciones';
    this.stateChangeSuccessMessage = undefined;
    this.stateChangeSuccessRoute = undefined;
    this.cdr.markForCheck();
    this.router.navigate([target]);
  }

  // Muestro el modal de confirmación antes de eliminar.
  protected openDeletePrompt(): void {
    if (!this.canDelete()) {
      return;
    }
    this.deletePromptVisible = true;
    this.deleteError = undefined;
  }

  // Cierro el modal sin eliminar nada.
  protected cancelDeletePrompt(): void {
    this.deletePromptVisible = false;
  }

  // Llamo al backend para eliminar la publicación y vuelvo al listado propio.
  protected confirmDelete(): void {
    if (!this.publication || this.isDeleting) {
      return;
    }
    this.isDeleting = true;
    this.deleteError = undefined;
    this.publicaciones.deletePublication(this.publication.id).subscribe({
      next: () => {
        this.isDeleting = false;
        this.deletePromptVisible = false;
        this.router.navigate(['/mis-publicaciones']);
      },
      error: () => {
        this.isDeleting = false;
        this.deleteError = 'No pudimos eliminar la publicación. Intentá nuevamente.';
      }
    });
  }
}

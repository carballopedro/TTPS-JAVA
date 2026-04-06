import { Component, OnInit, computed, effect, inject, signal } from '@angular/core';
import { NgIf, NgFor } from '@angular/common';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { PublicacionesService, UserPublication } from '../../services/publicaciones.service';

@Component({
  selector: 'app-mispublicaciones',
  standalone: true,
  imports: [NgIf, NgFor],
  templateUrl: './mispublicaciones.html',
  styleUrl: './mispublicaciones.css',
})
export class MispublicacionesComponent implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly publicaciones = inject(PublicacionesService);
  private readonly router = inject(Router);

  protected readonly user = this.auth.currentUser;
  protected readonly defaultImage = 'pet-placeholder.svg'; // misma imagen que en mascotas
  protected readonly isLoading = signal(false); // flag para mostrar estado de carga
  protected readonly items = signal<UserPublication[]>([]); // publicaciones del usuario
  protected readonly hasPublications = computed(() => this.items().length > 0);
  protected readonly formatPetStatus = (value?: string | null) => {
    if (!value) {
      return 'Sin estado';
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
    return value.charAt(0) + value.slice(1).toLowerCase();
  };
  protected readonly capitalize = (value?: string | null) => {
    if (!value) {
      return 'Sin datos';
    }
    const lower = value.toLowerCase();
    return lower.charAt(0).toUpperCase() + lower.slice(1);
  };

  // Este efecto se ejecuta cada vez que cambia el usuario autenticado y dispara la carga.
  private readonly loadPublicationsEffect = effect(
    () => {
      const current = this.user();
      if (!current) {
        this.items.set([]);
        return;
      }
      this.fetchUserPublications(current.id);
    },
    { allowSignalWrites: true }
  );

  // al montar el componente me aseguro de tener el perfil cargado
  ngOnInit(): void {
    this.auth.ensureProfileLoaded();
  }

  // pide las publicaciones propias al backend y actualiza el loading
  private fetchUserPublications(userId: number): void {
    this.isLoading.set(true);
    // Hit al endpoint de publicaciones filtradas por usuario.
    this.publicaciones.getByUser(userId).subscribe({
      next: (publications) => {
        const parsed = Array.isArray(publications) ? publications : [];
        this.items.set(parsed);
        this.isLoading.set(false);
      },
      error: () => {
        this.items.set([]);
        this.isLoading.set(false);
      }
    });
  }

  // normaliza cualquier fecha al formato dd/mm/aaaa
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

  // abre el detalle privado enviando todos los datos en el state
  protected viewDetails(publication: UserPublication): void {
    // Navego al detalle enviando la publicación completa para no volver a pedirla.
    this.router.navigate(['/publicacion-detalle', publication.id], {
      state: { publication }
    });
  }

  // shortcut para ir al formulario de nueva publicación
  protected createPublication(): void {
    this.router.navigateByUrl('/publicacion');
  }
}

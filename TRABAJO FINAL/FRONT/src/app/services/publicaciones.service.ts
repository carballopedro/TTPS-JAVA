import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';

// DTO base que esperamos del backend para cada publicación.
export interface PublicationPet {
  nombre?: string;
  color?: string;
  tipo?: string;
  tamanio?: string;
  estado?: string;
  raza?: string;
  fotoUrl?: string;
}

// representa cada foto que viene del back
export interface PublicationPhoto {
  id: number;
}

export interface UserPublication {
  id: number;
  activa: boolean;
  titulo?: string;
  // Algunas llamadas devuelven la fecha como string y otras como arreglo.
  fecha: string | number[];
  latitud: number | null;
  longitud: number | null;
  creador_id: number;
  mascota?: PublicationPet | null;
  barrio: string;
  descripcion: string;
  ciudad: string;
  estado?: string;

  // la lista de fotos que viene de la entidad Publicacion
  fotos?: PublicationPhoto[];
}

@Injectable({
  providedIn: 'root',
})
export class PublicacionesService {
  private readonly baseUrl = 'http://localhost:8080/TrabajoFinalCarballo';
  private readonly userPublicationsUrl = `${this.baseUrl}/publicaciones/usuario`;
  private readonly publicationsUrl = `${this.baseUrl}/publicaciones`;

  constructor(private readonly http: HttpClient) {}

  // Traigo todas las publicaciones creadas por un usuario puntual.
  getByUser(userId: number): Observable<UserPublication[]> {
    return this.http.get<UserPublication[]>(`${this.userPublicationsUrl}/${userId}`).pipe(
      map((publications) => publications.map((publication) => withPublicationPhoto(this.baseUrl, publication)))
    );
  }

  // Crea una nueva publicación enviando los datos en multipart/form-data.
  // El backend responde con texto plano, por eso pido la respuesta como 'text' para evitar errores de parseo.
  createPublication(payload: FormData): Observable<string> {
    return this.http.post(this.publicationsUrl, payload, { responseType: 'text' });
  }

  // Trae una publicación puntual por ID para reutilizarla en el detalle aunque no haya navegación previa.
  getById(publicationId: number): Observable<UserPublication> {
    return this.http
      .get<UserPublication>(`${this.publicationsUrl}/${publicationId}`)
      .pipe(map((publication) => withPublicationPhoto(this.baseUrl, publication)));
  }

  // Borra una publicación existente usando el ID provisto por el detalle.
  deletePublication(publicationId: number): Observable<string> {
    return this.http.delete(`${this.publicationsUrl}/${publicationId}`, { responseType: 'text' });
  }

  // Actualiza descripción y nombre de la mascota enviando JSON plano.
  updatePublication(publicationId: number, payload: { descripcion: string; mascota: { nombre: string } }): Observable<string> {
    return this.http.put(`${this.publicationsUrl}/${publicationId}`, payload, { responseType: 'text' });
  }

  // Cambia el estado de la publicación enviando la acción solicitada (Adoptar/Recuperar).
  changePublicationState(publicationId: number, accion: 'Adoptar' | 'Recuperar'): Observable<string> {
    return this.http.put(`${this.publicationsUrl}/${publicationId}/estado`, null, {
      params: { accion },
      responseType: 'text',
    });
  }
}

// Devuelve una publicación con fotoUrl seteado si existen fotos asociadas.
export function withPublicationPhoto(baseUrl: string, publication: UserPublication): UserPublication {
  const fotos = publication.fotos ?? [];
  const fotoPrincipalId = fotos.length > 0 ? fotos[0].id : null;

  if (!fotoPrincipalId || !publication.mascota) {
    return publication;
  }

  return {
    ...publication,
    mascota: {
      ...publication.mascota,
      fotoUrl: `${baseUrl}/publicaciones/fotos/${fotoPrincipalId}`,
    },
  };
}

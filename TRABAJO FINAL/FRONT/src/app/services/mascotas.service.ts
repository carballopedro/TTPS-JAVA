import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { UserPublication, withPublicationPhoto } from './publicaciones.service';

export interface PublicationCreator {
  id: number;
  nombre: string;
  apellido: string;
  email: string;
  telefono: string;
}

// DTO que llega desde el endpoint listarPublicaciones.
export interface PublicationListResponse {
  publicacion: UserPublication;
  creadorId: number;
  creadorNombre: string;
  creadorApellido: string;
  creadorEmail: string;
  creadorTelefono: string;
}

// Modelo que usa el front para mostrar la tarjeta y el detalle.
export interface PublicationDetail {
  publication: UserPublication;
  creator: PublicationCreator;
}

@Injectable({
  providedIn: 'root',
})
export class MascotasService {
  private readonly baseUrl = 'http://localhost:8080/TrabajoFinalCarballo';
  private readonly publicationsUrl = `${this.baseUrl}/publicaciones/listarPublicaciones`;
  private readonly recentPublicationsUrl = `${this.baseUrl}/publicaciones/recientes`;

  constructor(private readonly http: HttpClient) {}

  // Devuelve todas las publicaciones disponibles junto con su creador.
  listPublications(): Observable<PublicationDetail[]> {
    return this.http.get<PublicationListResponse[]>(this.publicationsUrl).pipe(
      map((items) => items.map((item) => this.mapResponse(item)))
    );
  }

  // Devuelve las publicaciones ordenadas por fecha descendente aplicando un límite opcional.
  listRecentPublications(limit: number): Observable<PublicationDetail[]> {
    return this.http
      .get<PublicationListResponse[]>(this.recentPublicationsUrl, {
        params: { limit: String(limit) },
      })
      .pipe(map((items) => items.map((item) => this.mapResponse(item))));
  }

  // Normaliza la respuesta cruda del API a un modelo más cómodo para el front.
  private mapResponse(item: PublicationListResponse): PublicationDetail {
    const publication = withPublicationPhoto(this.baseUrl, item.publicacion);

    return {
      publication,
      creator: {
        id: item.creadorId,
        nombre: item.creadorNombre,
        apellido: item.creadorApellido,
        email: item.creadorEmail,
        telefono: item.creadorTelefono,
      },
    };
  }
}

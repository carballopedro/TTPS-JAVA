import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface RankedUser {
  id: number;
  nombre: string;
  apellido: string;
  barrio: string;
  ciudad: string;
  puntos: number;
  badges?: string[] | null;
}

@Injectable({
  providedIn: 'root',
})
export class RankingService {
  private readonly baseUrl = 'http://localhost:8080/TrabajoFinalCarballo';
  private readonly rankingUrl = `${this.baseUrl}/usuarios/ranking`;

  constructor(private readonly http: HttpClient) {}

  // Traigo el ranking de usuarios limitando la cantidad solicitada.
  getTopUsers(limit: number): Observable<RankedUser[]> {
    const params = new HttpParams().set('limit', limit);
    return this.http.get<RankedUser[]>(this.rankingUrl, { params });
  }
}

import { Injectable, computed, signal } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, map, tap } from 'rxjs';

// Respuesta mínima que necesito del servicio Nominatim
interface NominatimResponse {
  lat: string;
  lon: string;
  display_name: string;
  address?: {
    city?: string;
    town?: string;
    village?: string;
    suburb?: string;
    neighbourhood?: string;
    city_district?: string;
    state?: string;
    county?: string;
  };
} 

// Estructura de datos con la ubicación que uso en el resto de la app
export interface LocationInfo {
  city: string;
  neighborhood: string;
  latitude: number;
  longitude: number;
}

@Injectable({
  providedIn: 'root',
})
export class GeocodingService {
  // Endpoint público del servicio de geocodificación inversa
  private readonly baseUrl = 'https://nominatim.openstreetmap.org/reverse';
  // Signal para almacenar la última ubicación obtenida
  private readonly lastLocationSignal = signal<LocationInfo | null>(null);

  // Computed que expone la última ubicación al resto de componentes
  readonly lastLocation = computed(() => this.lastLocationSignal());

  constructor(private readonly http: HttpClient) {}

  // Llama al API de Nominatim para traducir lat/lon en ciudad y barrio
  lookup(lat: number, lon: number): Observable<LocationInfo> {
    const params = new HttpParams()
      .set('format', 'jsonv2')
      .set('lat', lat)
      .set('lon', lon)
      .set('zoom', 16)
      .set('addressdetails', '1');

    return this.http.get<NominatimResponse>(this.baseUrl, {
      params,
      headers: {
        'Accept-Language': 'es',
      },
    })
      .pipe(
        map((response) => this.mapResponse(lat, lon, response)),
        tap((location) => this.lastLocationSignal.set(location)),
      );
  }

  // Normaliza la respuesta del API para quedarme con lo que necesito
  private mapResponse(lat: number, lon: number, response: NominatimResponse): LocationInfo {
    const address = response.address ?? {};
    // asigno barrio; si no viene, asingo suburbios, sino town/village para tener algún identificador de zona.
    const neighborhoodCandidate =
      address.neighbourhood ??
      address.suburb ??
      address.city_district ??
      address.town ??
      address.village ??
      '';
    const neighborhood = neighborhoodCandidate?.trim();

    // asigno ciudad, si no viene asigno town/village, sino county/state, así siempre queda un valor
    const city = address.city ?? address.town ?? address.village ?? address.county ?? address.state ?? '';

    return {
      city,
      neighborhood: neighborhood || 'Centro', // asigno Centro si no llega ningún barrio
      latitude: lat,
      longitude: lon,
    };
  }
}

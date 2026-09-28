import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL, API_KEY } from './config';

export interface TelemetryReading {
  id: number;
  recordedAt: string;
  cpuPercent: number;
  ramPercent: number;
}

@Injectable({ providedIn: 'root' })
export class TelemetryService {
  constructor(private http: HttpClient) {}

  getReadings(): Observable<TelemetryReading[]> {
    const headers = new HttpHeaders({ 'X-API-Key': API_KEY });
    return this.http.get<TelemetryReading[]>(`${API_BASE_URL}/telemetry`, { headers });
  }
}

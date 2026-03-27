import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface GuardarSemanaRequest {
  fechaInicio:      string;
  fechaFin:         string;
  horaInicio:       string;
  horaFin:          string;
  duracionMinutos:  number;
  lugarDefecto?:    string;
  observaciones?:   string;
  idPeriodo?:       number;
}

export interface SemanaPredefensaDto {
  idSemana:         number;
  fechaInicio:      string;
  fechaFin:         string;
  horaInicio:       string;
  horaFin:          string;
  duracionMinutos:  number;
  lugarDefecto?:    string;
  observaciones?:   string;
  periodoCodigo?:   string;
  activo:           boolean;
  totalSlots:       number;
  slotsOcupados:    number;
  slotsLibres:      number;
}

export interface SlotDto {
  fechaSlot:       string;
  horaInicio:      string;
  horaFin:         string;
  ocupado:         boolean;
  idSustentacion?: number;
  idProyecto?:     number;
  tituloProyecto?: string;
  estudiante?:     string;
  lugar?:          string;
}

export interface DiaCalendarioDto {
  fecha:      string;
  diaSemana:  string;
  slots:      SlotDto[];
  totalSlots: number;
  ocupados:   number;
  libres:     number;
}

export interface CalendarioSemanaDto {
  semana: SemanaPredefensaDto;
  dias:   DiaCalendarioDto[];
}

export interface AsignarSlotRequest {
  idProyecto:      number;
  fecha:           string;
  hora:            string;
  lugar?:          string;
  observaciones?:  string;
  idRealizadoPor:  number;
}

export interface ExtenderSemanaRequest {
  fechaFin?:        string;
  horaInicio?:      string;
  horaFin?:         string;
  duracionMinutos?: number;
  lugarDefecto?:    string;
  observaciones?:   string;
}

@Injectable({ providedIn: 'root' })
export class SemanaPredefensaService {

  private readonly API = `${environment.apiUrl}/api/dt2/semana-predefensa`;

  constructor(private http: HttpClient) {}

  obtenerSemana(): Observable<SemanaPredefensaDto> {
    return this.http.get<SemanaPredefensaDto>(this.API);
  }

  guardarSemana(req: GuardarSemanaRequest): Observable<SemanaPredefensaDto> {
    return this.http.post<SemanaPredefensaDto>(this.API, req);
  }

  obtenerCalendario(): Observable<CalendarioSemanaDto> {
    return this.http.get<CalendarioSemanaDto>(`${this.API}/calendario`);
  }

  extenderSemana(req: ExtenderSemanaRequest): Observable<SemanaPredefensaDto> {
    return this.http.patch<SemanaPredefensaDto>(`${this.API}/extender`, req);
  }

  asignarSlot(req: AsignarSlotRequest): Observable<any> {
    return this.http.post<any>(`${this.API}/asignar`, req);
  }
}

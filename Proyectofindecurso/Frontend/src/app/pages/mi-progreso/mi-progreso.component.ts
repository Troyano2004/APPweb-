import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { getSessionUser, getSessionEntityId } from '../../services/session';

export interface EtapaDto {
  clave:       string;
  titulo:      string;
  descripcion: string;
  estado:      'COMPLETADO' | 'EN_CURSO' | 'PENDIENTE' | 'RECHAZADO';
  fecha:       string | null;
}

export interface EstadoEstudianteDto {
  nombreCompleto:   string;
  carrera:          string;
  modalidad:        string;
  etapaActual:      string;
  porcentajeAvance: number;
  etapas:           EtapaDto[];
}

@Component({
  selector: 'app-mi-progreso',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './mi-progreso.component.html',
  styleUrl:    './mi-progreso.component.css',
})
export class MiProgresoComponent implements OnInit {

  estado:   EstadoEstudianteDto | null = null;
  cargando  = true;
  error:    string | null = null;

  private readonly API = 'http://localhost:8080/api/estado-estudiante';

  constructor(private http: HttpClient) {}

  ngOnInit(): void {
    const user = getSessionUser();
    const idEstudiante = getSessionEntityId(user, 'estudiante');

    if (!idEstudiante) {
      this.error    = 'No se pudo identificar al estudiante. Cierra sesión e ingresa de nuevo.';
      this.cargando = false;
      return;
    }

    this.http
      .get<EstadoEstudianteDto>(`${this.API}/${idEstudiante}`, { withCredentials: true })
      .subscribe({
        next:  data  => { this.estado = data; this.cargando = false; },
        error: ()    => { this.error  = 'No se pudo cargar tu progreso. Intenta más tarde.'; this.cargando = false; },
      });
  }
  etapasCompletadas(): number {
    return this.estado?.etapas.filter(e => e.estado === 'COMPLETADO').length ?? 0;
  }

  etapasPendientes(): number {
    return this.estado?.etapas.filter(e => e.estado === 'PENDIENTE').length ?? 0;
  }

  dotClass(estado: string): string {
    const mapa: Record<string, string> = {
      COMPLETADO: 'dot-done',
      EN_CURSO:   'dot-cur',
      RECHAZADO:  'dot-rej',
      PENDIENTE:  'dot-pend',
    };
    return mapa[estado] ?? 'dot-pend';
  }

  badgeClass(estado: string): string {
    const mapa: Record<string, string> = {
      COMPLETADO: 'badge-done',
      EN_CURSO:   'badge-cur',
      RECHAZADO:  'badge-rej',
      PENDIENTE:  'badge-pend',
    };
    return mapa[estado] ?? 'badge-pend';
  }

  connClass(estado: string): string {
    const mapa: Record<string, string> = {
      COMPLETADO: 'done',
      EN_CURSO:   'cur',
    };
    return mapa[estado] ?? '';
  }
}

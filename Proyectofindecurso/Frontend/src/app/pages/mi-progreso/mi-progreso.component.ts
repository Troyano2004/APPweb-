import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { getSessionUser } from '../../services/session';

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
    const idEstudiante = user?.['idEstudiante'] ?? user?.['idUsuario'];

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

  iconoClase(estado: string): string {
    const mapa: Record<string, string> = {
      COMPLETADO: 'estado-completado',
      EN_CURSO:   'estado-en-curso',
      RECHAZADO:  'estado-rechazado',
      PENDIENTE:  'estado-pendiente',
    };
    return mapa[estado] ?? 'estado-pendiente';
  }
}

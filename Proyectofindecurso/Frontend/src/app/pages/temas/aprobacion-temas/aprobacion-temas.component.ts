import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ComisionTemasService, PropuestaTemaDto } from '../../../services/comision-temas';
import { getSessionEntityId, getSessionUser } from '../../../services/session';

@Component({
  selector: 'app-aprobacion-temas',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './aprobacion-temas.component.html',
  styleUrl: './aprobacion-temas.component.scss'
})
export class AprobacionTemasComponent implements OnInit {
  idDocente = getSessionEntityId(getSessionUser(), 'docente');

  propuestas = signal<PropuestaTemaDto[]>([]);
  loading = signal(false);
  error = signal<string | null>(null);

  observaciones: Record<number, string> = {};

  constructor(private readonly api: ComisionTemasService) {}

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    if (!this.idDocente) {
      this.error.set('No se pudo identificar al docente de comisión.');
      return;
    }

    this.loading.set(true);
    this.error.set(null);
    this.api.listarPropuestasComision(this.idDocente).subscribe({
      next: (resp) => {
        this.propuestas.set(resp ?? []);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(err?.error?.message ?? 'No se pudo cargar propuestas.');
        this.loading.set(false);
      }
    });
  }

  decidir(propuesta: PropuestaTemaDto, estado: 'APROBADA' | 'RECHAZADA'): void {
    if (!this.idDocente) return;
    this.api.decidirPropuesta(this.idDocente, propuesta.idPropuesta, estado, this.observaciones[propuesta.idPropuesta] ?? '').subscribe({
      next: () => this.cargar(),
      error: (err) => this.error.set(err?.error?.message ?? 'No se pudo registrar la decisión.')
    });
  }
}

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
  procesandoId = signal<number | null>(null);

  observaciones: Record<number, string> = {};
  propuestasDecididas = new Set<number>();

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
        const propuestas = resp ?? [];
        this.propuestas.set(propuestas);
        this.propuestasDecididas = new Set(
          propuestas
            .filter((propuesta) => propuesta.estado === 'APROBADA' || propuesta.estado === 'RECHAZADA')
            .map((propuesta) => propuesta.idPropuesta)
        );
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(err?.error?.message ?? 'No se pudo cargar propuestas.');
        this.loading.set(false);
      }
    });
  }

  estaBloqueada(propuesta: PropuestaTemaDto): boolean {
    return this.procesandoId() === propuesta.idPropuesta || this.propuestasDecididas.has(propuesta.idPropuesta);
  }

  decidir(propuesta: PropuestaTemaDto, estado: 'APROBADA' | 'RECHAZADA'): void {
    if (!this.idDocente || this.estaBloqueada(propuesta)) return;

    this.procesandoId.set(propuesta.idPropuesta);
    this.error.set(null);

    this.api.decidirPropuesta(this.idDocente, propuesta.idPropuesta, estado, this.observaciones[propuesta.idPropuesta] ?? '').subscribe({
      next: () => {
        this.propuestasDecididas.add(propuesta.idPropuesta);
        this.propuestas.update((list) =>
          list.map((item) =>
            item.idPropuesta === propuesta.idPropuesta
              ? { ...item, estado, observaciones: this.observaciones[propuesta.idPropuesta] ?? item.observaciones }
              : item
          )
        );
        this.procesandoId.set(null);
      },
      error: (err) => {
        this.error.set(err?.error?.message ?? 'No se pudo registrar la decisión.');
        this.procesandoId.set(null);
      }
    });
  }
}

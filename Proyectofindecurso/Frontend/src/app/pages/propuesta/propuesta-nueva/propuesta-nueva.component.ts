import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ComisionTemasService, PropuestaTemaDto, TemaBancoDto } from '../../../services/comision-temas';
import { getSessionEntityId, getSessionUser } from '../../../services/session';

@Component({
  selector: 'app-propuesta-nueva',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './propuesta-nueva.component.html',
  styleUrl: './propuesta-nueva.component.scss'
})
export class PropuestaNuevaComponent implements OnInit {
  idEstudiante = getSessionEntityId(getSessionUser(), 'estudiante');

  historial = signal<PropuestaTemaDto[]>([]);
  temasDisponibles = signal<TemaBancoDto[]>([]);
  loading = signal(false);
  loadingTemas = signal(false);
  saving = signal(false);
  error = signal<string | null>(null);
  ok = signal<string | null>(null);

  form = {
    idCarrera: 1,
    idTema: null as number | null,
    titulo: '',
    temaInvestigacion: '',
    planteamientoProblema: '',
    objetivosGenerales: '',
    objetivosEspecificos: '',
    metodologia: '',
    resultadosEsperados: '',
    bibliografia: ''
  };

  constructor(private readonly api: ComisionTemasService) {}

  ngOnInit(): void {
    this.cargarTemasDisponibles();
    this.cargarHistorial();
  }

  cargarTemasDisponibles(): void {
    if (!this.idEstudiante) {
      this.error.set('No se pudo identificar al estudiante autenticado.');
      return;
    }

    this.loadingTemas.set(true);
    this.api.listarTemasDisponiblesEstudiante(this.idEstudiante).subscribe({
      next: (resp) => {
        this.temasDisponibles.set(resp ?? []);
        this.loadingTemas.set(false);
      },
      error: (err) => {
        this.error.set(err?.error?.message ?? 'No se pudo cargar los temas disponibles.');
        this.loadingTemas.set(false);
      }
    });
  }

  onSeleccionTema(): void {
    const idTema = this.form.idTema;
    if (!idTema) return;

    const tema = this.temasDisponibles().find((t) => t.idTema === idTema);
    if (!tema) return;

    if (!this.form.titulo.trim()) {
      this.form.titulo = tema.titulo;
    }
    if (!this.form.temaInvestigacion.trim()) {
      this.form.temaInvestigacion = tema.titulo;
    }
  }

  cargarHistorial(): void {
    if (!this.idEstudiante) {
      this.error.set('No se pudo identificar al estudiante autenticado.');
      return;
    }

    this.loading.set(true);
    this.api.listarPropuestasEstudiante(this.idEstudiante).subscribe({
      next: (resp) => {
        this.historial.set(resp ?? []);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(err?.error?.message ?? 'No se pudo cargar historial de propuestas.');
        this.loading.set(false);
      }
    });
  }

  enviar(): void {
    if (!this.idEstudiante) {
      this.error.set('No se pudo identificar al estudiante autenticado.');
      return;
    }

    if (!this.form.titulo.trim()) {
      this.error.set('El título es obligatorio.');
      return;
    }

    this.saving.set(true);
    this.error.set(null);
    this.ok.set(null);

    this.api
      .crearPropuestaEstudiante(this.idEstudiante, {
        idCarrera: this.form.idCarrera,
        idTema: this.form.idTema ?? undefined,
        titulo: this.form.titulo,
        temaInvestigacion: this.form.temaInvestigacion,
        planteamientoProblema: this.form.planteamientoProblema,
        objetivosGenerales: this.form.objetivosGenerales,
        objetivosEspecificos: this.form.objetivosEspecificos,
        metodologia: this.form.metodologia,
        resultadosEsperados: this.form.resultadosEsperados,
        bibliografia: this.form.bibliografia
      })
      .subscribe({
        next: () => {
          this.ok.set('Tu propuesta fue enviada a la comisión para revisión.');
          this.form = {
            idCarrera: this.form.idCarrera,
            idTema: null,
            titulo: '',
            temaInvestigacion: '',
            planteamientoProblema: '',
            objetivosGenerales: '',
            objetivosEspecificos: '',
            metodologia: '',
            resultadosEsperados: '',
            bibliografia: ''
          };
          this.saving.set(false);
          this.cargarHistorial();
        },
        error: (err) => {
          this.error.set(err?.error?.message ?? 'No se pudo enviar la propuesta.');
          this.saving.set(false);
        }
      });
  }
}

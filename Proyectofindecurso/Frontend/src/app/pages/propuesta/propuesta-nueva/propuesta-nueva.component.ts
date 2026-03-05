import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  ComisionTemasService,
  EstadoModalidadDto,
  PropuestaTemaDto,
  TemaBancoDto
} from '../../../services/comision-temas';
import { CatalogoCarrera, CatalogosService } from '../../../services/catalogos';
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
  loadingCarreras = signal(false);
  loadingModalidad = signal(false);
  saving = signal(false);
  guardandoModalidad = signal(false);
  error = signal<string | null>(null);
  ok = signal<string | null>(null);
  estadoModalidad = signal<EstadoModalidadDto | null>(null);
  modalidadSeleccionada = signal<number | null>(null);
  carreras = signal<CatalogoCarrera[]>([]);

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

  constructor(
    private readonly api: ComisionTemasService,
    private readonly catalogosApi: CatalogosService
  ) {}

  ngOnInit(): void {
    this.cargarEstadoModalidad();
    this.cargarCarreras();
    this.cargarTemasDisponibles();
    this.cargarHistorial();
  }

  get nombreCarreraSeleccionada(): string {
    const idCarrera = this.form.idCarrera;
    if (!idCarrera) return '';
    return this.carreras().find((c) => c.idCarrera === idCarrera)?.nombre ?? '';
  }

  get temasFiltradosPorCarrera(): TemaBancoDto[] {
    const nombreCarrera = this.normalizarTexto(this.nombreCarreraSeleccionada);
    if (!nombreCarrera) return this.temasDisponibles();

    return this.temasDisponibles().filter((tema) => this.normalizarTexto(tema.carrera) === nombreCarrera);
  }

  get tieneModalidadSeleccionada(): boolean {
    if (this.loadingModalidad()) {
      return false;
    }
    return this.estadoModalidad()?.tieneModalidad ?? false;
  }

  cargarEstadoModalidad(): void {
    if (!this.idEstudiante) {
      this.error.set('No se pudo identificar al estudiante autenticado.');
      return;
    }

    this.loadingModalidad.set(true);
    this.api.obtenerEstadoModalidad(this.idEstudiante).subscribe({
      next: (estado) => {
        this.estadoModalidad.set(estado);
        this.modalidadSeleccionada.set(estado.idModalidad);
        if (estado.idCarrera) {
          this.form.idCarrera = estado.idCarrera;
        }
        this.onSeleccionCarrera();
        this.loadingModalidad.set(false);
      },
      error: (err) => {
        this.error.set(err?.error?.message ?? 'No se pudo validar la modalidad de titulación.');
        this.loadingModalidad.set(false);
      }
    });
  }

  cargarCarreras(): void {
    this.loadingCarreras.set(true);
    this.catalogosApi.listarCarreras().subscribe({
      next: (resp) => {
        const carreras = resp ?? [];
        this.carreras.set(carreras);

        if (!carreras.some((c) => c.idCarrera === this.form.idCarrera)) {
          this.form.idCarrera = carreras[0]?.idCarrera ?? this.form.idCarrera;
        }

        this.onSeleccionCarrera();
        this.loadingCarreras.set(false);
      },
      error: (err) => {
        this.error.set(err?.error?.message ?? 'No se pudo cargar el catálogo de carreras.');
        this.loadingCarreras.set(false);
      }
    });
  }

  guardarModalidad(): void {
    if (!this.idEstudiante) {
      this.error.set('No se pudo identificar al estudiante autenticado.');
      return;
    }

    const idModalidad = this.modalidadSeleccionada();
    if (!idModalidad) {
      this.error.set('Selecciona una modalidad para continuar.');
      return;
    }

    this.guardandoModalidad.set(true);
    this.error.set(null);
    this.ok.set(null);

    this.api.seleccionarModalidad(this.idEstudiante, idModalidad).subscribe({
      next: (estado) => {
        this.estadoModalidad.set(estado);
        this.ok.set(`Modalidad registrada correctamente: ${estado.modalidad}. Ya puedes enviar tu propuesta.`);
        this.guardandoModalidad.set(false);
      },
      error: (err) => {
        this.error.set(err?.error?.message ?? 'No se pudo guardar la modalidad seleccionada.');
        this.guardandoModalidad.set(false);
      }
    });
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

  onSeleccionCarrera(): void {
    const temaSeleccionado = this.temasFiltradosPorCarrera.some((tema) => tema.idTema === this.form.idTema);
    if (!temaSeleccionado) {
      this.form.idTema = null;
    }
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

    if (!this.tieneModalidadSeleccionada) {
      this.error.set('Antes de registrar la propuesta debes seleccionar tu modalidad de titulación.');
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
          this.cargarEstadoModalidad();
        }
      });
  }

  private normalizarTexto(valor: string | null | undefined): string {
    return (valor ?? '')
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .trim()
      .toLowerCase();
  }
}

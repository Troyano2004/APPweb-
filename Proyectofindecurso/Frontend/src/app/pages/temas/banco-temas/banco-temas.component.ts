import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ComisionTemasService, TemaBancoDto } from '../../../services/comision-temas';
import { CatalogoCarrera, CatalogosService } from '../../../services/catalogos';
import { getSessionEntityId, getSessionUser } from '../../../services/session';

@Component({
  selector: 'app-banco-temas',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './banco-temas.component.html',
  styleUrl: './banco-temas.component.scss'
})
export class BancoTemasComponent implements OnInit {
  idDocente = getSessionEntityId(getSessionUser(), 'docente');

  temas = signal<TemaBancoDto[]>([]);
  loading = signal(false);
  loadingCarreras = signal(false);
  saving = signal(false);
  error = signal<string | null>(null);
  ok = signal<string | null>(null);
  carreras = signal<CatalogoCarrera[]>([]);

  form = {
    idCarrera: 1,
    titulo: '',
    descripcion: '',
    observaciones: ''
  };

  constructor(
    private readonly api: ComisionTemasService,
    private readonly catalogosApi: CatalogosService
  ) {}

  ngOnInit(): void {
    this.cargarCarreras();
    this.cargar();
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

        this.loadingCarreras.set(false);
      },
      error: (err) => {
        this.error.set(err?.error?.message ?? 'No se pudo cargar el catálogo de carreras.');
        this.loadingCarreras.set(false);
      }
    });
  }

  cargar(): void {
    if (!this.idDocente) {
      this.error.set('No se pudo identificar al docente de comisión.');
      return;
    }

    this.loading.set(true);
    this.error.set(null);
    this.api.listarBanco(this.idDocente).subscribe({
      next: (resp) => {
        this.temas.set(resp ?? []);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(err?.error?.message ?? 'No se pudo cargar el banco de temas.');
        this.loading.set(false);
      }
    });
  }

  guardarTema(): void {
    if (!this.idDocente) {
      this.error.set('No se pudo identificar al docente de comisión.');
      return;
    }

    if (!this.form.titulo.trim() || !this.form.descripcion.trim()) {
      this.error.set('Título y descripción son obligatorios.');
      return;
    }

    this.saving.set(true);
    this.error.set(null);
    this.ok.set(null);

    this.api
      .crearTema(this.idDocente, {
        idCarrera: this.form.idCarrera,
        titulo: this.form.titulo,
        descripcion: this.form.descripcion,
        observaciones: this.form.observaciones
      })
      .subscribe({
        next: () => {
          this.ok.set('Tema agregado correctamente al banco.');
          this.form.titulo = '';
          this.form.descripcion = '';
          this.form.observaciones = '';
          this.saving.set(false);
          this.cargar();
        },
        error: (err) => {
          this.error.set(err?.error?.message ?? 'No se pudo guardar el tema.');
          this.saving.set(false);
        }
      });
  }
}

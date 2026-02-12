import { Component, OnInit, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  CarreraModalidadDto,
  CatalogoCarrera,
  CatalogoModalidad,
  CatalogosService
} from '../../../services/catalogos';
import { getSessionUser, hasRole } from '../../../services/session';

@Component({
  selector: 'app-carrera-modalidad',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './carrera-modalidad.component.html',
  styleUrl: './carrera-modalidad.component.scss'
})
export class CarreraModalidadComponent implements OnInit {
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly error = signal<string | null>(null);
  readonly ok = signal<string | null>(null);

  readonly carreras = signal<CatalogoCarrera[]>([]);
  readonly modalidades = signal<CatalogoModalidad[]>([]);
  readonly relaciones = signal<CarreraModalidadDto[]>([]);

  readonly idCarrera = signal<number | null>(null);
  readonly idModalidad = signal<number | null>(null);

  readonly canManage = computed(() => {
    const user = getSessionUser();
    return hasRole(user?.rol, 'ROLE_COORDINADOR', 'ROLE_ADMIN');
  });

  readonly relacionesActivas = computed(() => this.relaciones().filter((r) => r.activo));

  constructor(private readonly catalogosApi: CatalogosService) {}

  ngOnInit(): void {
    this.cargarDatos();
  }

  cargarDatos(): void {
    this.loading.set(true);
    this.error.set(null);

    this.catalogosApi.listarCarreras().subscribe({
      next: (carreras) => {
        this.carreras.set(carreras ?? []);
        this.catalogosApi.listarModalidades().subscribe({
          next: (modalidades) => {
            this.modalidades.set(modalidades ?? []);
            this.catalogosApi.listarCarreraModalidad().subscribe({
              next: (relaciones) => {
                this.relaciones.set(relaciones ?? []);
                this.loading.set(false);
              },
              error: (err) => {
                this.error.set(err?.error?.message ?? 'No se pudo cargar las asignaciones carrera-modalidad.');
                this.loading.set(false);
              }
            });
          },
          error: (err) => {
            this.error.set(err?.error?.message ?? 'No se pudo cargar las modalidades de titulación.');
            this.loading.set(false);
          }
        });
      },
      error: (err) => {
        this.error.set(err?.error?.message ?? 'No se pudo cargar las carreras.');
        this.loading.set(false);
      }
    });
  }

  existeRelacion(idCarrera: number, idModalidad: number): boolean {
    return this.relaciones().some((r) => r.idCarrera === idCarrera && r.idModalidad === idModalidad && r.activo);
  }

  guardar(): void {
    if (!this.canManage()) {
      this.error.set('No tienes permisos para gestionar este catálogo.');
      return;
    }

    const carrera = this.idCarrera();
    const modalidad = this.idModalidad();

    if (!carrera || !modalidad) {
      this.error.set('Debes seleccionar una carrera y una modalidad.');
      return;
    }

    if (this.existeRelacion(carrera, modalidad)) {
      this.error.set('Esta modalidad ya está disponible para la carrera seleccionada.');
      return;
    }

    this.saving.set(true);
    this.error.set(null);
    this.ok.set(null);

    this.catalogosApi.asignarCarreraModalidad(carrera, modalidad).subscribe({
      next: () => {
        this.ok.set('Asignación guardada correctamente.');
        this.idModalidad.set(null);
        this.saving.set(false);
        this.cargarDatos();
      },
      error: (err) => {
        this.error.set(err?.error?.message ?? 'No se pudo guardar la asignación.');
        this.saving.set(false);
      }
    });
  }
}

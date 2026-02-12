import { Component, OnInit, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CatalogoModalidad, CatalogosService } from '../../../services/catalogos';
import { getSessionUser, hasRole } from '../../../services/session';

@Component({
  selector: 'app-modalidad-catalogo',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './modalidad-catalogo.component.html',
  styleUrl: './modalidad-catalogo.component.scss'
})
export class ModalidadCatalogoComponent implements OnInit {
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly error = signal<string | null>(null);
  readonly ok = signal<string | null>(null);

  readonly modalidades = signal<CatalogoModalidad[]>([]);
  readonly nombreNueva = signal('');

  readonly canManage = computed(() => {
    const user = getSessionUser();
    return hasRole(user?.rol, 'ROLE_COORDINADOR', 'ROLE_ADMIN');
  });

  constructor(private readonly catalogosApi: CatalogosService) {}

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.loading.set(true);
    this.error.set(null);

    this.catalogosApi.listarModalidades().subscribe({
      next: (data) => {
        this.modalidades.set(data ?? []);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(err?.error?.message ?? 'No se pudo cargar modalidades.');
        this.loading.set(false);
      }
    });
  }

  guardar(): void {
    if (!this.canManage()) {
      this.error.set('No tienes permisos para gestionar modalidades.');
      return;
    }

    const nombre = this.nombreNueva().trim();
    if (!nombre) {
      this.error.set('El nombre de modalidad es obligatorio.');
      return;
    }

    const existe = this.modalidades().some((m) => (m.nombre || '').trim().toUpperCase() === nombre.toUpperCase());
    if (existe) {
      this.error.set('Esa modalidad ya existe.');
      return;
    }

    this.saving.set(true);
    this.error.set(null);
    this.ok.set(null);

    this.catalogosApi.crearModalidad(nombre).subscribe({
      next: () => {
        this.ok.set('Modalidad creada correctamente.');
        this.nombreNueva.set('');
        this.saving.set(false);
        this.cargar();
      },
      error: (err) => {
        this.error.set(err?.error?.message ?? 'No se pudo crear la modalidad.');
        this.saving.set(false);
      }
    });
  }
}

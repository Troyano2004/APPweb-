import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { RevisionDirectorService, DocumentoPendienteDto } from '../../../services/revision-director';
import { getSessionEntityId, getSessionUser } from '../../../services/session';

@Component({
  selector: 'app-revision',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './revision.html',
  styleUrl: './revision.scss'
})
export class Revision implements OnInit {
  idDocente = getSessionEntityId(getSessionUser(), 'docente');

  loading = signal(false);
  error = signal<string | null>(null);
  pendientes = signal<DocumentoPendienteDto[]>([]);

  constructor(private api: RevisionDirectorService) {}

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    if (!this.idDocente) {
      this.error.set('No se pudo identificar al docente autenticado.');
      return;
    }

    this.loading.set(true);
    this.error.set(null);

    this.api.pendientes(this.idDocente).subscribe({
      next: (list) => {
        this.pendientes.set(list ?? []);
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err?.error?.message ?? 'Error cargando pendientes');
      }
    });
  }
}

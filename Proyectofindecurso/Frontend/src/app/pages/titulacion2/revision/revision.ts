import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { RevisionDirectorService, DocumentoPendienteDto } from '../../../services/revision-director';

@Component({
  selector: 'app-revision',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './revision.html',
  styleUrl: './revision.scss'
})
export class Revision implements OnInit {
  // por ahora fijo a 1 (luego sale del login)
  idDocente = 1;

  loading = signal(false);
  error = signal<string | null>(null);
  pendientes = signal<DocumentoPendienteDto[]>([]);

  constructor(private api: RevisionDirectorService) {}

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
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

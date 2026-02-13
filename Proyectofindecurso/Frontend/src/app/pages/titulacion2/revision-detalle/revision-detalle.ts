import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { DocumentoTitulacionService, DocumentoTitulacionDto } from '../../../services/documento-titulacion';

import { RevisionDirectorService, ObservacionDto } from '../../../services/revision-director';
import { getSessionEntityId, getSessionUser } from '../../../services/session';

@Component({
  selector: 'app-revision-detalle',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './revision-detalle.html',
  styleUrl: './revision-detalle.scss'
})
export class RevisionDetalle implements OnInit {
  idDocente = getSessionEntityId(getSessionUser(), 'docente');

  idDocumento = 0;
  documento = signal<DocumentoTitulacionDto | null>(null);

  titulo = signal<string>('');

  loading = signal(false);
  error = signal<string | null>(null);
  observaciones = signal<ObservacionDto[]>([]);

  // form simple (sin reactive forms para que sea rápido)
  seccion = 'METODOLOGIA';
  comentario = '';

  constructor(
    private route: ActivatedRoute,
    private api: RevisionDirectorService,
    private docApi: DocumentoTitulacionService
  ) {}

  ngOnInit(): void {
    const idDocParam = Number(this.route.snapshot.paramMap.get('idDocumento'));

    this.idDocumento = Number.isFinite(idDocParam) && idDocParam > 0 ? idDocParam : 0;

    if (!this.idDocumento) {
      this.error.set('No se envió idDocumento en la URL.');
      return;
    }

    const t = this.route.snapshot.queryParamMap.get('titulo');
    if (t) this.titulo.set(t);

    this.cargarDocumento();
    this.cargarObs();
  }

  cargarObs(): void {
    this.loading.set(true);
    this.error.set(null);

    this.api.observaciones(this.idDocumento).subscribe({
      next: (list) => {
        this.observaciones.set(list ?? []);
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err?.error?.message ?? 'Error cargando observaciones');
      }
    });
  }
  cargarDocumento(): void {
    this.loading.set(true);
    this.error.set(null);

    this.docApi.getDocumentoPorId(this.idDocumento).subscribe({
      next: (doc) => {
        this.documento.set(doc);
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err?.error?.message ?? 'Error cargando documento del estudiante');
      }
    });
  }

  agregar(): void {
    if (!this.comentario.trim() || !this.idDocente) return;

    this.loading.set(true);
    this.error.set(null);

    this.api.agregarObservacion(this.idDocente, this.idDocumento, {
      seccion: this.seccion,
      comentario: this.comentario,
      idAutor: this.idDocente
    }).subscribe({
      next: () => {
        this.comentario = '';
        this.cargarObs();
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err?.error?.message ?? 'Error agregando observación');
      }
    });
  }

  devolver(): void {
    if (!this.idDocente) return;
    this.loading.set(true);
    this.error.set(null);

    this.api.devolver(this.idDocente, this.idDocumento).subscribe({
      next: () => this.cargarObs(),
      error: (err) => {
        this.loading.set(false);
        this.error.set(err?.error?.message ?? 'Error devolviendo documento');
      }
    });
  }

  aprobar(): void {
    if (!this.idDocente) return;
    this.loading.set(true);
    this.error.set(null);

    this.api.aprobar(this.idDocente, this.idDocumento).subscribe({
      next: () => this.cargarObs(),
      error: (err) => {
        this.loading.set(false);
        this.error.set(err?.error?.message ?? 'Error aprobando documento');
      }
    });
  }

}

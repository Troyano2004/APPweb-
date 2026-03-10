import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs/operators';
import {
  Dt2Service,
  ProyectoPendienteConfiguracionDto,
  CertificadoAntiplacioDto
} from '../../../services/dt2.service';
import { getSessionUser, getSessionEntityId } from '../../../services/session';

@Component({
  selector: 'app-antiplagio-dt2',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './antiplagio-dt2.html',
  styleUrl: './antiplagio-dt2.scss'
})
export class AntiplagioDt2Component implements OnInit {
  loading = signal(false);
  error = signal<string | null>(null);
  ok = signal<string | null>(null);

  proyectos = signal<ProyectoPendienteConfiguracionDto[]>([]);
  proyectoSeleccionado = signal<ProyectoPendienteConfiguracionDto | null>(null);
  certificado = signal<CertificadoAntiplacioDto | null>(null);

  archivoSeleccionado: File | null = null;
  formAntiplagio: FormGroup;

  // ✅ CAMBIO: idDocenteDt2 en lugar de idDirector
  private idDocenteDt2 = 0;

  constructor(private dt2: Dt2Service, private fb: FormBuilder) {
    this.formAntiplagio = this.fb.group({
      porcentajeCoincidencia: [null, [Validators.required, Validators.min(0), Validators.max(100)]],
      observaciones: ['']
    });
  }

  ngOnInit(): void {
    const user = getSessionUser();
    // ✅ CAMBIO: sigue siendo entidad docente pero semánticamente es el DT2
    this.idDocenteDt2 = getSessionEntityId(user, 'docente') ?? 0;
    this.cargarProyectos();
  }

  seleccionarProyecto(p: ProyectoPendienteConfiguracionDto): void {
    this.proyectoSeleccionado.set(p);
    this.error.set(null);
    this.ok.set(null);
    this.archivoSeleccionado = null;
    this.formAntiplagio.reset();
    this.cargarCertificado(p.idProyecto);
  }

  onArchivoChange(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files?.length) {
      const f = input.files[0];
      if (f.type !== 'application/pdf') {
        this.error.set('Solo se permiten archivos PDF.');
        this.archivoSeleccionado = null;
        input.value = '';
        return;
      }
      if (f.size > 50 * 1024 * 1024) {
        this.error.set('El archivo no puede superar 50MB.');
        this.archivoSeleccionado = null;
        input.value = '';
        return;
      }
      this.archivoSeleccionado = f;
      this.error.set(null);
    }
  }

  subirAntiplagio(): void {
    const p = this.proyectoSeleccionado();
    if (!p) return;

    if (!this.archivoSeleccionado) {
      this.error.set('Debes seleccionar el informe COMPILATIO en PDF.');
      return;
    }
    if (this.formAntiplagio.invalid) {
      this.formAntiplagio.markAllAsTouched();
      return;
    }

    const v = this.formAntiplagio.value;
    const fd = new FormData();
    fd.append('archivo', this.archivoSeleccionado);
    // ✅ CAMBIO: idDocenteDt2 en lugar de idDirector
    fd.append('idDocenteDt2', String(this.idDocenteDt2));
    fd.append('porcentajeCoincidencia', String(v.porcentajeCoincidencia));
    if (v.observaciones) fd.append('observaciones', v.observaciones);

    this.loading.set(true);
    this.error.set(null);
    this.ok.set(null);

    this.dt2.registrarAntiplagio(p.idProyecto, fd)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (cert) => {
          this.certificado.set(cert);
          // ✅ CAMBIO: mensaje actualizado — ahora el documento pasa a ANTIPLAGIO_APROBADO
          const estado = cert.certificadoFavorable
            ? '✓ FAVORABLE — el documento pasa a estado ANTIPLAGIO_APROBADO. El coordinador puede programar la predefensa.'
            : '✗ RECHAZADO — porcentaje >= 10%. Debe corregir el documento y volver a subir el informe.';
          this.ok.set(`Informe registrado. ${estado}`);
          this.archivoSeleccionado = null;
          this.formAntiplagio.reset();
          // Recargar proyectos por si el estado del documento cambió
          this.cargarProyectos();
        },
        error: (err: any) => this.error.set(
          err?.error?.mensaje ?? err?.error?.message ?? 'Error al registrar informe antiplagio'
        )
      });
  }

  private cargarProyectos(): void {
    this.loading.set(true);
    // ✅ CAMBIO: listarProyectosDocenteDt2 en lugar de listarProyectosDirector
    // Este endpoint devuelve los proyectos donde el docente está asignado como DT2
    this.dt2.listarProyectosDocenteDt2(this.idDocenteDt2)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: data => this.proyectos.set(data),
        error: () => this.error.set('Error al cargar proyectos')
      });
  }

  private cargarCertificado(idProyecto: number): void {
    this.dt2.getCertificado(idProyecto).subscribe({
      next: data => this.certificado.set(data),
      error: () => this.certificado.set(null)
    });
  }
}

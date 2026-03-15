import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ZoomConfigService } from './service';
import { ZoomConfigDto } from './model';
import { getSessionUser, getSessionEntityId } from '../../services/session';

@Component({
  selector: 'app-zoom-config',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './zoom-config.html',
  styleUrls: ['./zoom-config.scss']
})
export class ZoomConfigComponent implements OnInit {

  config: ZoomConfigDto | null = null;
  idDocente: number | null = null;

  form: FormGroup;
  cargando = false;
  guardando = false;
  eliminando = false;
  mostrarSecret = false;
  editando = false;

  exito = '';
  error = '';

  constructor(
    private service: ZoomConfigService,
    private fb: FormBuilder,
    private cdr: ChangeDetectorRef
  ) {
    this.form = this.fb.group({
      accountId:    ['', Validators.required],
      clientId:     ['', Validators.required],
      clientSecret: ['', Validators.required]
    });
  }

  ngOnInit(): void {
    const user = getSessionUser();
    this.idDocente = getSessionEntityId(user, 'docente');
    if (this.idDocente) this.cargar();
  }

  cargar(): void {
    this.cargando = true;
    this.cdr.detectChanges();
    this.service.obtener(this.idDocente!).subscribe({
      next: data => {
        this.config = data;
        this.cargando = false;
        if (data.configurado) {
          this.form.patchValue({
            accountId: data.accountId,
            clientId:  data.clientId,
            clientSecret: ''
          });
        }
        this.cdr.detectChanges();
      },
      error: () => {
        this.cargando = false;
        this.cdr.detectChanges();
      }
    });
  }

  guardar(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid) return;
    this.guardando = true;
    this.limpiarAlertas();
    this.service.guardar(this.idDocente!, this.form.value).subscribe({
      next: data => {
        this.config = data;
        this.guardando = false;
        this.editando = false;
        this.mostrarExito('Configuración de Zoom guardada correctamente.');
        this.cdr.detectChanges();
      },
      error: (e) => {
        this.guardando = false;
        this.error = e?.error?.message ?? 'No se pudo guardar la configuración.';
        setTimeout(() => this.error = '', 5000);
        this.cdr.detectChanges();
      }
    });
  }

  eliminar(): void {
    if (!confirm('¿Eliminar la configuración de Zoom?')) return;
    this.eliminando = true;
    this.service.eliminar(this.idDocente!).subscribe({
      next: () => {
        this.config = null;
        this.eliminando = false;
        this.editando = false;
        this.form.reset();
        this.mostrarExito('Configuración eliminada.');
        this.cdr.detectChanges();
      },
      error: () => {
        this.eliminando = false;
        this.mostrarError('No se pudo eliminar la configuración.');
      }
    });
  }

  activarEdicion(): void {
    this.editando = true;
    this.form.get('clientSecret')?.setValue('');
    this.cdr.detectChanges();
  }

  cancelarEdicion(): void {
    this.editando = false;
    this.cdr.detectChanges();
  }

  toggleSecret(): void { this.mostrarSecret = !this.mostrarSecret; }

  invalido(campo: string): boolean {
    const ctrl = this.form.get(campo);
    return !!(ctrl?.invalid && ctrl?.touched);
  }

  private mostrarExito(msg: string): void {
    this.exito = msg; this.error = '';
    setTimeout(() => this.exito = '', 5000);
  }
  private mostrarError(msg: string): void {
    this.error = msg; this.exito = '';
    setTimeout(() => this.error = '', 5000);
  }
  private limpiarAlertas(): void { this.exito = ''; this.error = ''; }
}

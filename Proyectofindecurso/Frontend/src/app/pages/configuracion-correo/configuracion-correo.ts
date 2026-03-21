import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ConfiguracionCorreoService } from './service';
import { ConfiguracionCorreoDto } from './model';


@Component({
  selector: 'app-configuracion-correo',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './configuracion-correo.html',
  styleUrls: ['./configuracion-correo.scss']
})
export class ConfiguracionCorreoComponent implements OnInit {

  form!: FormGroup;
  configuraciones: ConfiguracionCorreoDto[] = [];

  cargando = true;
  guardando = false;
  procesando: number | null = null;
  mostrarFormulario = false;
  mostrarPassword = false;
  editandoId: number | null = null;

  exito = '';
  error = '';

  proveedores = [
    { value: 'GMAIL',   label: 'Gmail',   icon: '✉',  smtp: 'smtp.gmail.com',       logo: 'https://www.gstatic.com/images/branding/product/1x/gmail_2020q4_32dp.png' },
    { value: 'YAHOO',   label: 'Yahoo',   icon: '📨', smtp: 'smtp.mail.yahoo.com',  logo: 'https://img.icons8.com/color/32/yahoo.png' }
  ];

  constructor(
    private fb: FormBuilder,
    private service: ConfiguracionCorreoService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.initForm();
    this.cargar();
  }

  private initForm(data?: ConfiguracionCorreoDto): void {
    this.form = this.fb.group({
      proveedor: [data?.proveedor ?? '', Validators.required],
      usuario:   [data?.usuario   ?? '', [Validators.required, Validators.email]],
      password:  ['', this.editandoId ? [] : [Validators.required]]
    });
  }

  cargar(): void {
    this.cargando = true;
    this.cdr.detectChanges();
    this.service.listarTodas().subscribe({
      next: (lista) => {
        this.configuraciones = lista;
        this.cargando = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.configuraciones = [];
        this.cargando = false;
        this.cdr.detectChanges();
      }
    });
  }

  abrirFormulario(): void {
    this.editandoId = null;
    this.mostrarPassword = false;
    this.initForm();
    this.mostrarFormulario = true;
  }

  abrirEdicion(cfg: ConfiguracionCorreoDto): void {
    this.editandoId = cfg.id!;
    this.mostrarPassword = false;
    this.initForm(cfg);
    this.form.get('password')?.clearValidators();
    this.form.get('password')?.updateValueAndValidity();
    this.mostrarFormulario = true;
  }

  cerrarFormulario(): void {
    this.mostrarFormulario = false;
    this.editandoId = null;
    this.limpiarAlertas();
  }

  guardar(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid) return;
    this.guardando = true;
    this.limpiarAlertas();

    const dto: ConfiguracionCorreoDto = { ...this.form.value };
    if (this.editandoId && !dto.password?.trim()) delete dto.password;

    const op$ = this.editandoId
      ? this.service.editar(this.editandoId, dto)
      : this.service.crear(dto);

    op$.subscribe({
      next: () => {
        this.guardando = false;
        this.cerrarFormulario();
        this.mostrarExito(this.editandoId ? 'Configuración actualizada.' : 'Configuración creada correctamente.');
        this.cargar();
      },
      error: (err) => {
        this.guardando = false;
        const msg = err?.error?.message ?? err?.message ?? 'Error al guardar.';
        this.mostrarError(msg);
      }
    });
  }

  activar(cfg: ConfiguracionCorreoDto): void {
    this.procesando = cfg.id!;
    this.limpiarAlertas();
    this.service.activar(cfg.id!).subscribe({
      next: () => {
        this.procesando = null;
        this.mostrarExito(`"${cfg.usuario}" ahora es la configuración activa.`);
        this.cargar();
      },
      error: () => {
        this.procesando = null;
        this.mostrarError('Error al activar la configuración.');
      }
    });
  }

  eliminar(cfg: ConfiguracionCorreoDto): void {
    if (!confirm(`¿Eliminar la configuración de ${cfg.usuario}?`)) return;
    this.procesando = cfg.id!;
    this.service.eliminar(cfg.id!).subscribe({
      next: () => {
        this.procesando = null;
        this.mostrarExito('Configuración eliminada.');
        this.cargar();
      },
      error: (err) => {
        this.procesando = null;
        const msg = err?.error?.message ?? 'No se pudo eliminar.';
        this.mostrarError(msg);
      }
    });
  }

  togglePassword(): void { this.mostrarPassword = !this.mostrarPassword; }

  invalido(campo: string): boolean {
    const ctrl = this.form.get(campo);
    return !!(ctrl?.invalid && ctrl?.touched);
  }

  getProveedorLogo(proveedor: string): string {
    return this.proveedores.find(p => p.value === proveedor)?.logo ?? '';
  }
  getProveedorIcon(proveedor: string): string {
    return this.proveedores.find(p => p.value === proveedor)?.icon ?? '✉';
  }
  getProveedorLabel(proveedor: string): string {
    return this.proveedores.find(p => p.value === proveedor)?.label ?? proveedor;
  }
  getProveedorSmtp(proveedor: string): string {
    return this.proveedores.find(p => p.value === proveedor)?.smtp ?? '';
  }

  private mostrarExito(msg: string): void {
    this.exito = msg; this.error = '';
    setTimeout(() => this.exito = '', 5000);
  }
  private mostrarError(msg: string): void {
    this.error = msg; this.exito = '';
    setTimeout(() => this.error = '', 6000);
  }
  private limpiarAlertas(): void { this.exito = ''; this.error = ''; }
}

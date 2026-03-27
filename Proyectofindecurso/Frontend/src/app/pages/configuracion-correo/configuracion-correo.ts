import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
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
  autorizando = false;
  procesando: number | null = null;
  mostrarFormulario = false;
  mostrarPassword = false;
  editandoId: number | null = null;

  exito = '';
  error = '';

  proveedores = [
    { value: 'GMAIL',   label: 'Gmail',   icon: '✉',  smtp: 'smtp.gmail.com',        logo: 'https://www.gstatic.com/images/branding/product/1x/gmail_2020q4_32dp.png' },
    { value: 'YAHOO',   label: 'Yahoo',   icon: '📨', smtp: 'smtp.mail.yahoo.com',   logo: 'https://img.icons8.com/color/32/yahoo.png' },
    { value: 'OUTLOOK', label: 'Outlook', icon: '📧', smtp: 'smtp-mail.outlook.com', logo: 'https://img.icons8.com/color/32/microsoft-outlook-2019.png' }
  ];

  constructor(
    private fb: FormBuilder,
    private service: ConfiguracionCorreoService,
    private route: ActivatedRoute,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.initForm();
    this.cargar();
    // Detectar si Microsoft redirigió con ?outlook=autorizado
    this.route.queryParams.subscribe(params => {
      if (params['outlook'] === 'autorizado') {
        this.mostrarExito('Cuenta Outlook autorizada correctamente.');
        this.cargar();
      }
      if (params['outlook'] === 'error') {
        this.mostrarError('Error al autorizar la cuenta Outlook.');
      }
    });
  }

  get esOutlook(): boolean {
    return this.form.get('proveedor')?.value === 'OUTLOOK';
  }

  private initForm(data?: ConfiguracionCorreoDto): void {
    this.form = this.fb.group({
      proveedor:    [data?.proveedor    ?? '', Validators.required],
      usuario:      [data?.usuario      ?? '', [Validators.required, Validators.email]],
      password:     [''],
      clientId:     [data?.clientId     ?? ''],
      clientSecret: ['']
    });
    this.actualizarValidadores();
    this.form.get('proveedor')?.valueChanges.subscribe(() => this.actualizarValidadores());
  }

  private actualizarValidadores(): void {
    const esOutlook = this.form.get('proveedor')?.value === 'OUTLOOK';
    const passwordCtrl     = this.form.get('password');
    const clientIdCtrl     = this.form.get('clientId');
    const clientSecretCtrl = this.form.get('clientSecret');

    if (esOutlook) {
      passwordCtrl?.clearValidators();
      clientIdCtrl?.setValidators(Validators.required);
      clientSecretCtrl?.setValidators(this.editandoId ? [] : [Validators.required]);
    } else {
      clientIdCtrl?.clearValidators();
      clientSecretCtrl?.clearValidators();
      passwordCtrl?.setValidators(this.editandoId ? [] : [Validators.required]);
    }

    passwordCtrl?.updateValueAndValidity();
    clientIdCtrl?.updateValueAndValidity();
    clientSecretCtrl?.updateValueAndValidity();
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

    // Limpiar campos vacíos opcionales
    if (!dto.password?.trim())     delete dto.password;
    if (!dto.clientSecret?.trim()) delete dto.clientSecret;

    const op$ = this.editandoId
      ? this.service.editar(this.editandoId, dto)
      : this.service.crear(dto);

    op$.subscribe({
      next: (cfg) => {
        this.guardando = false;
        const guardadoId = cfg.id;
        this.cerrarFormulario();
        this.mostrarExito(this.editandoId ? 'Configuración actualizada.' : 'Configuración creada.');
        this.cargar();

        // Si es Outlook nuevo, lanzar autorización automáticamente
        if (!this.editandoId && dto.proveedor === 'OUTLOOK' && guardadoId) {
          setTimeout(() => this.autorizarOutlook(guardadoId), 800);
        }
      },
      error: (err) => {
        this.guardando = false;
        const msg = err?.error?.message ?? err?.message ?? 'Error al guardar.';
        this.mostrarError(msg);
      }
    });
  }
  autorizarOutlook(id: number): void {
    this.autorizando = true;
    this.service.autorizarOutlook(id).subscribe({
      next: ({ url }) => {
        this.autorizando = false;
        window.location.href = url;  // ← misma pestaña
      },
      error: () => {
        this.autorizando = false;
        this.mostrarError('Error al generar link de autorización.');
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
      error: (err) => {
        this.procesando = null;
        const msg = err?.error?.message ?? 'Error al activar.';
        this.mostrarError(msg);
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

  getProveedorLogo(p: string)  { return this.proveedores.find(x => x.value === p)?.logo  ?? ''; }
  getProveedorIcon(p: string)  { return this.proveedores.find(x => x.value === p)?.icon  ?? '✉'; }
  getProveedorLabel(p: string) { return this.proveedores.find(x => x.value === p)?.label ?? p; }
  getProveedorSmtp(p: string)  { return this.proveedores.find(x => x.value === p)?.smtp  ?? ''; }

  private mostrarExito(msg: string): void {
    this.exito = msg; this.error = '';
    setTimeout(() => { this.exito = ''; this.cdr.detectChanges(); }, 5000);
  }
  private mostrarError(msg: string): void {
    this.error = msg; this.exito = '';
    setTimeout(() => { this.error = ''; this.cdr.detectChanges(); }, 6000);
  }
  private limpiarAlertas(): void { this.exito = ''; this.error = ''; }
}

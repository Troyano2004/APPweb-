import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { SolicitudRegistroService } from './service';
import { CarreraItem } from './model';
import { ChangeDetectorRef } from '@angular/core';

@Component({
  selector: 'app-registro-estudiante',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './registro-estudiante.html',
  styleUrls: ['./registro-estudiante.scss']
})
export class RegistroEstudianteComponent implements OnInit {

  paso = 1; // 1=correo, 2=código, 3=datos, 4=éxito
  cargando = false;
  error = '';
  carreras: CarreraItem[] = [];

  // Paso 1
  formCorreo: FormGroup;

  // Paso 2
  formCodigo: FormGroup;

  // Paso 3
  formDatos: FormGroup;

  constructor(
    private fb: FormBuilder,
    private svc: SolicitudRegistroService,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {
    this.formCorreo = this.fb.group({
      correo: ['', [Validators.required, Validators.email]]
    });

    this.formCodigo = this.fb.group({
      codigo: ['', [Validators.required, Validators.minLength(6), Validators.maxLength(6)]]
    });

    this.formDatos = this.fb.group({
      cedula:    ['', Validators.required],
      nombres:   ['', Validators.required],
      apellidos: ['', Validators.required],
      idCarrera: [null, Validators.required]
    });
  }

  ngOnInit() {
    this.svc.listarCarreras().subscribe({
      next: (data) => this.carreras = data,
      error: () => this.error = 'No se pudieron cargar las carreras.'
    });
  }

  // PASO 1
  enviarCorreo() {
    this.error = '';
    if (this.formCorreo.invalid) { this.formCorreo.markAllAsTouched(); return; }
    this.cargando = true;
    const { correo } = this.formCorreo.value;
    this.svc.enviarCorreo(correo).subscribe({
      next: () => { this.paso = 2; this.cargando = false;  this.cdr.detectChanges();},
      error: (err) => { this.error = this.mensajeError(err); this.cargando = false; }
    });
  }

  // PASO 2
  verificarCodigo() {
    this.error = '';
    if (this.formCodigo.invalid) { this.formCodigo.markAllAsTouched(); return; }
    this.cargando = true;
    this.svc.verificarCodigo({
      correo: this.formCorreo.value.correo,
      codigo: this.formCodigo.value.codigo
    }).subscribe({
      next: () => { this.paso = 3; this.cargando = false; this.cdr.detectChanges(); },
      error: (err) => { this.error = this.mensajeError(err); this.cargando = false; }
    });
  }

  // PASO 3
  enviarDatos() {
    this.error = '';
    if (this.formDatos.invalid) { this.formDatos.markAllAsTouched(); return; }
    this.cargando = true;
    this.svc.enviarDatos({
      correo: this.formCorreo.value.correo,
      ...this.formDatos.value
    }).subscribe({
      next: () => { this.paso = 4; this.cargando = false; this.cdr.detectChanges(); },
      error: (err) => {
        this.error = this.mensajeError(err);
        this.cargando = false;
        this.cdr.detectChanges(); // ✅ agregar aquí
      }
    });
  }

  irLogin() { this.router.navigate(['/login']); }

  // Helpers para mostrar errores por campo en el HTML
  invalido(form: FormGroup, campo: string): boolean {
    const ctrl = form.get(campo);
    return !!(ctrl && ctrl.invalid && ctrl.touched);
  }

  private mensajeError(err: any): string {
    const msg = err?.error?.message || err?.message || '';
    const mensajes: Record<string, string> = {
      'CORREO_REQUERIDO':                   'El correo es obligatorio.',
      'SOLICITUD_YA_APROBADA':              'Ya existe una cuenta con ese correo.',
      'CODIGO_INCORRECTO':                  'El código ingresado es incorrecto.',
      'CODIGO_REQUERIDO':                   'El código es obligatorio.',
      'SOLICITUD_NO_EXISTE':                'No se encontró una solicitud para ese correo.',
      'PRIMERO_VERIFICA_CORREO':            'Primero debes verificar tu correo.',
      'FALTAN_DATOS':                       'Completa todos los campos.',
      'YA_EXISTE_USUARIO_CON_ESE_CORREO':   'Ya existe un usuario con ese correo.',
      'YA_EXISTE_USUARIO_CON_ESTA_CEDULA': 'Ya existe una solicitud con esa cédula.',
      'CARRERA_NO_EXISTE':                  'La carrera seleccionada no existe.',
    };
    return mensajes[msg] ?? 'Ocurrió un error. Intenta nuevamente.';
  }
}

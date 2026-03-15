import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { GestionCoordinadoresService } from './service';
import { CoordinadorAdminResponse, CarreraItem } from './model';
import {form} from '@angular/forms/signals';

@Component({
  selector: 'app-gestion-coordinadores',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './gestion-coordinadores.html',
  styleUrls: ['./gestion-coordinadores.scss']
})
export class GestionCoordinadores implements OnInit{
  coordinadores: CoordinadorAdminResponse [] = [];
  usuariosCoordinador:CoordinadorAdminResponse []= [];
  carreras: CarreraItem[] = [];
  cargando= false;
  exito = ""
  error = "";

  mostrarModal = false;

  form: FormGroup;

  constructor(private service: GestionCoordinadoresService, private cdr:ChangeDetectorRef,private fb: FormBuilder) {
    this.form = this.fb.group({
        idUsuario: [null, Validators.required],
        idCarrera: [null, Validators.required]
      }
    )

  }
  ngOnInit() {
    this.cargarCoordinadores();
    this.cargarCarreras();
    this.cargarUsuarios();
  }
  cargarCoordinadores():void{
    this.cargando = true;
    this.cdr.detectChanges();
    this.service.listar().subscribe({
      next:data =>{
        this.coordinadores = data;
        this.cargando = false;
        this.cdr.detectChanges();
      },
      error: err => {
        this.error =  'No se pudo cargar los coordinadores.';
        this.cargando = false;
        this.cdr.detectChanges();
      }
    })
  }
  cargarCarreras():void{
    this.cargando = true;
    this.cdr.detectChanges();
    this.service.listarCarreras().subscribe({
      next: data => {
        this.carreras = data;
        this.cargando = false
        this.cdr.detectChanges();
      },
      error:() => {
        this.error = 'No se pueden cargar las carreras';
        this.cdr.detectChanges();

    }
    })
  }
  cargarUsuarios():void {
    this.cargando = true;
    this.cdr.detectChanges();
    this.service.listarUsuariosCoordinador().subscribe({
      next:data => {
        this.cargando = false;
        this.usuariosCoordinador = data;
        this.cdr.detectChanges();
      }
    })
  }
  abrirModal(): void {
    this.form.reset();
    this.mostrarModal = true;
    this.cdr.detectChanges();
  }

  cerrarModal(): void {
    this.mostrarModal = false;
    this.cdr.detectChanges();
  }

  asignar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.cargando = true;
    this.service.asignar(this.form.value).subscribe({
      next: () => {
        this.cargando = false;
        this.exito = 'Coordinador asignado correctamente.';
        setTimeout(() => this.exito = '', 4000);
        this.cerrarModal();
        this.cargarCoordinadores()
        this.cargarUsuarios();
      },
      error: (e) => {
        this.cargando = false;
        this.error = e?.error || 'No se pudo asignar el coordinador.';
        setTimeout(() => this.error = '', 4000);
      }
    });
  }

  cambiarEstado(c: CoordinadorAdminResponse): void {
    const accion = c.activo ? 'desactivar' : 'activar';
    if (!confirm(`¿Desea ${accion} a ${c.nombres} ${c.apellidos}?`)) return;
    this.service.cambiarEstado(c.idCoordinador, !c.activo).subscribe({
      next: () => {
        this.exito = `Coordinador ${accion === 'activar' ? 'activado' : 'desactivado'} correctamente.`;
        setTimeout(() => this.exito = '', 4000);
        this.cargarCoordinadores();
        this.cargarUsuarios();
      },
      error: (e) => {
        this.error = e?.error || `No se pudo ${accion} el coordinador.`;
        setTimeout(() => this.error = '', 4000);
      }
    });
  }
}

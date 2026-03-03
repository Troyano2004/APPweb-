import {ChangeDetectorRef, Component, OnInit} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs/operators';

import {
  CoordinadorService,
  Dt1AsignacionCreateRequest,
  Dt1AsignarTutorRequest,
  InformacionAcademicaDt1
} from '../../../services/coordinador';

import { getSessionUser } from '../../../services/session';

type Opcion = { id: number; nombre: string };
type MsgType = 'ok' | 'error' | '';

@Component({
  selector: 'app-asignacion-dt1',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './asignacion-dt1.html',
  styleUrls: ['./asignacion-dt1.scss']
})
export class AsignacionDt1 implements OnInit {
  tab: 'docentes' | 'tutores' = 'docentes';

  cargando = false;

  mensaje = '';
  msgType: MsgType = '';

  carreraNombre = '---';
  periodoNombre = '---';

  private idCarrera: number | null = null;
  private idPeriodo: number | null = null;

  // ✅ combos correctos
  docentesCarrera: Opcion[] = []; // TAB 1 (habilitar DT1)
  docentesDt1: Opcion[] = [];     // TAB 2 (asignar tutor)
  estudiantes: Opcion[] = [];     // TAB 2 (estudiantes sin tutor)

  formDocente: FormGroup;
  formTutor: FormGroup;

  constructor(private api: CoordinadorService, private fb: FormBuilder, private cdr: ChangeDetectorRef) {
    // ✅ formularios simples
    this.formDocente = this.fb.group({
      idDocente: [null, Validators.required]
    });

    this.formTutor = this.fb.group({
      idEstudiante: [null, Validators.required],
      idDocente: [null, Validators.required]
    });
  }
  ngOnInit(): void {
    this.intentarCargarInfo(5); // intenta 5 veces
  }

  private intentarCargarInfo(intentos: number) {
    const idUsuario = this.getIdUsuarioFromSession();

    if (!idUsuario) {
      if (intentos <= 0) {
        this.mensaje = 'No se encontró idUsuario en sesión. Vuelve a iniciar sesión.';
        this.msgType = 'error';
        return;
      }

      // espera un poco y vuelve a intentar
      setTimeout(() => this.intentarCargarInfo(intentos - 1), 300);
      return;
    }

    // si ya hay idUsuario, ahora sí carga normal
    this.cargarInformacionConId(idUsuario);
  }


  private cargarInformacionConId(idUsuario: number): void {
    this.limpiarMensaje();
    this.cargando = true;

    this.api.getInformacionAcademicaDt1(idUsuario)
      .pipe(finalize(() => (this.cargando = false)))
      .subscribe({
        next: (info) => {
          this.idCarrera = info.idCarrera;
          this.idPeriodo = info.idPeriodoAcademico;

          this.carreraNombre = info.carrera || '---';
          this.periodoNombre = info.periodoAcademico || '---';

          this.docentesCarrera = (info.docentesCarrera || []).map(d => ({ id: d.idDocente, nombre: d.nombre }));
          this.docentesDt1 = (info.docentesDt1 || []).map(d => ({ id: d.idDocente, nombre: d.nombre }));
          this.estudiantes = (info.estudiantesDisponibles || []).map(e => ({ id: e.idEstudiante, nombre: e.nombre }));

          this.formDocente.reset({ idDocente: null });
          this.formTutor.reset({ idEstudiante: null, idDocente: null });
        },
        error: (e) => {
          this.mensaje = this.errMsg(e);
          this.msgType = 'error';
        }
      });
  }
  cambiarTab(t: 'docentes' | 'tutores') {
    this.tab = t;
    this.limpiarMensaje();
  }

  private limpiarMensaje() {
    this.mensaje = '';
    this.msgType = '';
  }

  // ✅ FIX TS4111: usar bracket
  private getIdUsuarioFromSession(): number | null {
    const user: any = getSessionUser();
    const id =
      user?.['idUsuario'] ??
      user?.['id_usuario'] ??
      user?.['id'] ??
      null;

    const n = Number(id);
    if (!n || !Number.isFinite(n)) return null;
    return n;
  }

  private cargarInformacion(): void {
    this.limpiarMensaje();

    const idUsuario = this.getIdUsuarioFromSession();
    if (!idUsuario) {
      this.mensaje = 'No se encontró idUsuario en sesión. Revisa tu login (guardar idUsuario).';
      this.msgType = 'error';
      return;
    }

    this.cargando = true;

    this.api.getInformacionAcademicaDt1(idUsuario)
      .pipe(finalize(() => (this.cargando = false)))
      .subscribe({
        next: (info: InformacionAcademicaDt1) => {
          // cabecera
          this.idCarrera = info.idCarrera;
          this.idPeriodo = info.idPeriodoAcademico;
          this.carreraNombre = info.carrera || '---';
          this.periodoNombre = info.periodoAcademico || '---';

          // TAB 1: docentes de la carrera
          this.docentesCarrera = (info.docentesCarrera || []).map(d => ({
            id: d.idDocente,
            nombre: d.nombre
          }));

          // TAB 2: docentes DT1 ya habilitados
          this.docentesDt1 = (info.docentesDt1 || []).map(d => ({
            id: d.idDocente,
            nombre: d.nombre
          }));

          // TAB 2: estudiantes sin tutor
          this.estudiantes = (info.estudiantesDisponibles || []).map(e => ({
            id: e.idEstudiante,
            nombre: e.nombre
          }));

          // reset suave
          this.formDocente.reset({ idDocente: null });
          this.formTutor.reset({ idEstudiante: null, idDocente: null });
        },
        error: (e) => {
          this.mensaje = this.errMsg(e);
          this.msgType = 'error';
        }
      });
  }

  // ====== TAB 1: Habilitar docente como DT1 ======
  habilitarDocenteDt1(): void {
    this.limpiarMensaje();

    if (!this.idCarrera || !this.idPeriodo) {
      this.mensaje = 'No hay carrera o periodo activo.';
      this.msgType = 'error';
      return;
    }

    if (this.formDocente.invalid) {
      this.mensaje = 'Selecciona un docente de la carrera.';
      this.msgType = 'error';
      this.formDocente.markAllAsTouched();
      return;
    }

    const idUsuario = this.getIdUsuarioFromSession();
    if (!idUsuario) {
      this.mensaje = 'No se encontró idUsuario en sesión.';
      this.msgType = 'error';
      return;
    }

    const idDocente = Number(this.formDocente.value.idDocente);

    const req: Dt1AsignacionCreateRequest = {
      idUsuario,
      idDocente,
      idCarrera: this.idCarrera,
      idPeriodo: this.idPeriodo
    };

    this.cargando = true;

    this.api.crearAsignacionDt1(req)
      .pipe(finalize(() => (this.cargando = false)))
      .subscribe({
        next: () => {
          this.mensaje = 'Docente habilitado como DT1 ✅';
          this.msgType = 'ok';
          this.cargarInformacion(); // refresca listas
        },
        error: (e) => {
          // ✅ “Asignacion existente”
          this.mensaje = this.errMsg(e);
          this.msgType = 'error';
        }
      });
  }

  // ====== TAB 2: Asignar tutor ======
  asignarTutor(): void {
    this.limpiarMensaje();

    if (!this.idPeriodo) {
      this.mensaje = 'No hay periodo activo.';
      this.msgType = 'error';
      return;
    }

    if (this.formTutor.invalid) {
      this.mensaje = 'Selecciona estudiante y docente DT1.';
      this.msgType = 'error';
      this.formTutor.markAllAsTouched();
      return;
    }

    const idUsuario = this.getIdUsuarioFromSession();
    if (!idUsuario) {
      this.mensaje = 'No se encontró idUsuario en sesión.';
      this.msgType = 'error';
      return;
    }

    const req: Dt1AsignarTutorRequest = {
      idUsuario,
      idEstudiante: Number(this.formTutor.value.idEstudiante),
      idDocente: Number(this.formTutor.value.idDocente),
      idPeriodo: this.idPeriodo
    };

    this.cargando = true;

    this.api.asignarTutorDt1(req)
      .pipe(finalize(() => (this.cargando = false)))
      .subscribe({
        next: () => {
          this.mensaje = 'Tutor asignado ✅';
          this.msgType = 'ok';
          this.cargarInformacion();
        },
        error: (e) => {
          this.mensaje = this.errMsg(e);
          this.msgType = 'error';
        }
      });
  }

  // ====== errores bonitos ======
  private errMsg(e: any): string {
    // cuando el backend manda texto directo
    if (typeof e?.error === 'string') return e.error;

    // cuando manda JSON {message:"..."}
    if (typeof e?.error?.message === 'string') return e.error.message;

    // status comunes
    if (e?.status === 409) return 'Ya existe una asignación para ese docente en este periodo.';
    if (e?.status === 400) return 'Datos inválidos. Revisa lo que estás enviando.';
    if (e?.status === 0) return 'No hay conexión con el backend (revisa que Spring esté corriendo).';

    return e?.message || 'Error';
  }
}

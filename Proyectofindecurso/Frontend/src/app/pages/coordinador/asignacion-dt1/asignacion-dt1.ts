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
  archivoNombre = '';
  carreraNombre = '---';
  periodoNombre = '---';

  private idCarrera: number | null = null;
  private idPeriodo: number | null = null;

  docentesCarrera: Opcion[] = [];
  docentesDt1: Opcion[] = [];
  estudiantes: Opcion[] = [];



  // 👇 AGREGA ESTO
  modoAsignacion: 'manual' | 'excel' = 'manual';

  archivoExcel: File | null = null;
  cedulasExcel: string[] = [];
  previsualizacion: { cedula: string }[] = [];
  advertenciasExcel: string[] = [];
  mostrarPrevisualizacion = false;

  formDocente: FormGroup;
  formTutor: FormGroup;

  private msgTimer: any;

  constructor(
    private api: CoordinadorService,
    private fb: FormBuilder,
    private cdr: ChangeDetectorRef
  ) {
    this.formDocente = this.fb.group({ idDocente: [null, Validators.required] });
    this.formTutor = this.fb.group({
      idEstudiante: [null, Validators.required],
      idDocente:    [null, Validators.required]
    });
  }

  ngOnInit(): void {
    this.intentarCargarInfo(5);
  }
  onArchivoSeleccionado(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files?.length) return;
    this.archivoNombre = input.files[0].name;
    this.archivoExcel = input.files[0];
    const archivo = input.files[0];

    // Si es CSV lee como texto
    if (archivo.name.endsWith('.csv')) {
      const reader = new FileReader();
      reader.onload = (e) => {
        const texto = e.target?.result as string;
        const lineas = texto.split('\n').map(l => l.trim()).filter(l => l);
        const datos = lineas.slice(1); // salta encabezado
        this.cedulasExcel = datos.map(l => l.split(',')[0].trim()).filter(c => c);
        this.previsualizacion = this.cedulasExcel.map(c => ({ cedula: c }));
        this.mostrarPrevisualizacion = true;
        this.cdr.detectChanges();
      };
      reader.readAsText(archivo);
    } else {
      // Si es XLSX usa SheetJS
      const reader = new FileReader();
      reader.onload = (e) => {
        const data = new Uint8Array(e.target?.result as ArrayBuffer);
        import('xlsx').then(XLSX => {
          const workbook = XLSX.read(data, { type: 'array' });
          const sheet = workbook.Sheets[workbook.SheetNames[0]];
          const rows: any[] = XLSX.utils.sheet_to_json(sheet, {
            header: 1,
            raw: false,      // ← lee todo como string
            defval: ''       // ← celdas vacías como string vacío
          });
          console.log('FILAS LEIDAS:', rows);
          const datos = rows.slice(1).filter((r: any) => r[0]); // salta encabezado
          this.cedulasExcel = datos
            .map((r: any) => String(r[0] || '').trim())
            .filter(c => c.length > 0);
          console.log('CEDULAS:', this.cedulasExcel);
          this.previsualizacion = this.cedulasExcel.map(c => ({ cedula: c }));
          this.mostrarPrevisualizacion = true;
          this.cdr.detectChanges();
        });
      };
      reader.readAsArrayBuffer(archivo);
    }
  }
  asignarDesdeExcel(): void {
    if (!this.cedulasExcel.length) {
      this.mostrarMensaje('No hay cédulas cargadas.', 'error');
      return;
    }
    if (!this.formTutor.get('idDocente')?.value) {
      this.mostrarMensaje('Selecciona un docente tutor.', 'error');
      return;
    }

    const idUsuario = this.getIdUsuarioFromSession();
    if (!idUsuario) return;

    this.cargando = true;
    this.api.asignarTutorExcel({
      idUsuario,
      idDocente: Number(this.formTutor.value.idDocente),
      cedulas: this.cedulasExcel
    }).pipe(finalize(() => { this.cargando = false; this.cdr.detectChanges(); }))
      .subscribe({
        next: (advertencias) => {
          this.advertenciasExcel = advertencias;
          this.mostrarMensaje(
            advertencias.length
              ? `Asignación completada con ${advertencias.length} advertencia(s).`
              : 'Todos los estudiantes fueron asignados correctamente.',
            advertencias.length ? 'error' : 'ok',
            6000
          );
          this.mostrarPrevisualizacion = false;
          this.cedulasExcel = [];
          this.cargarInformacion();
        },
        error: (e) => this.mostrarMensaje(this.errMsg(e), 'error')
      });
  }
  private intentarCargarInfo(intentos: number) {
    const idUsuario = this.getIdUsuarioFromSession();
    if (!idUsuario) {
      if (intentos <= 0) {
        this.mostrarMensaje('No se encontró idUsuario en sesión. Vuelve a iniciar sesión.', 'error');
        return;
      }
      setTimeout(() => this.intentarCargarInfo(intentos - 1), 300);
      return;
    }
    this.cargarInformacionConId(idUsuario);
  }

  private cargarInformacionConId(idUsuario: number): void {
    this.cargando = true;

    this.api.getInformacionAcademicaDt1(idUsuario)
      .pipe(finalize(() => { this.cargando = false; this.cdr.detectChanges(); }))
      .subscribe({
        next: (info) => {
          this.idCarrera = info.idCarrera;
          this.idPeriodo = info.idPeriodoAcademico;
          this.carreraNombre = info.carrera || '---';
          this.periodoNombre = info.periodoAcademico || '---';
          this.docentesCarrera = (info.docentesCarrera || []).map(d => ({ id: d.idDocente, nombre: d.nombre }));
          this.docentesDt1     = (info.docentesDt1 || []).map(d => ({ id: d.idDocente, nombre: d.nombre }));
          this.estudiantes     = (info.estudiantesDisponibles || []).map(e => ({ id: e.idEstudiante, nombre: e.nombre }));
          this.formDocente.reset({ idDocente: null });
          this.formTutor.reset({ idEstudiante: null, idDocente: null });
        },
        error: (e) => this.mostrarMensaje(this.errMsg(e), 'error')
      });
  }

  private cargarInformacion(): void {
    const idUsuario = this.getIdUsuarioFromSession();
    if (!idUsuario) {
      this.mostrarMensaje('No se encontró idUsuario en sesión.', 'error');
      return;
    }
    // No limpiar mensaje aquí — dejar que el mensaje de éxito sea visible
    this.cargarInformacionConId(idUsuario);
  }

  cambiarTab(t: 'docentes' | 'tutores') {
    this.tab = t;
    this.limpiarMensaje();
  }

  // ====== TAB 1: Habilitar docente como DT1 ======
  habilitarDocenteDt1(): void {
    this.limpiarMensaje();

    if (!this.idCarrera || !this.idPeriodo) {
      this.mostrarMensaje('No hay carrera o periodo activo.', 'error');
      return;
    }

    if (this.formDocente.invalid) {
      this.formDocente.markAllAsTouched();
      this.mostrarMensaje('Selecciona un docente de la carrera.', 'error');
      return;
    }

    const idUsuario = this.getIdUsuarioFromSession();
    if (!idUsuario) {
      this.mostrarMensaje('No se encontró idUsuario en sesión.', 'error');
      return;
    }

    const req: Dt1AsignacionCreateRequest = {
      idUsuario,
      idDocente: Number(this.formDocente.value.idDocente),
      idCarrera: this.idCarrera,
      idPeriodo: this.idPeriodo
    };

    this.cargando = true;

    this.api.crearAsignacionDt1(req)
      .pipe(finalize(() => { this.cargando = false; this.cdr.detectChanges(); }))
      .subscribe({
        next: () => {
          this.mostrarMensaje('Docente habilitado como DT1 correctamente.', 'ok', 4000);
          this.cargarInformacion();
        },
        error: (e) => this.mostrarMensaje(this.errMsg(e), 'error')
      });
  }

  // ====== TAB 2: Asignar tutor ======
  asignarTutor(): void {
    this.limpiarMensaje();

    if (!this.idPeriodo) {
      this.mostrarMensaje('No hay periodo activo.', 'error');
      return;
    }

    if (this.formTutor.invalid) {
      this.formTutor.markAllAsTouched();
      this.mostrarMensaje('Selecciona estudiante y docente DT1.', 'error');
      return;
    }

    const idUsuario = this.getIdUsuarioFromSession();
    if (!idUsuario) {
      this.mostrarMensaje('No se encontró idUsuario en sesión.', 'error');
      return;
    }

    const req: Dt1AsignarTutorRequest = {
      idUsuario,
      idEstudiante: Number(this.formTutor.value.idEstudiante),
      idDocente:    Number(this.formTutor.value.idDocente),
      idPeriodo:    this.idPeriodo
    };

    this.cargando = true;

    this.api.asignarTutorDt1(req)
      .pipe(finalize(() => { this.cargando = false; this.cdr.detectChanges(); }))
      .subscribe({
        next: () => {
          this.mostrarMensaje('Tutor asignado correctamente.', 'ok', 4000);
          this.cargarInformacion();
        },
        error: (e) => this.mostrarMensaje(this.errMsg(e), 'error')
      });
  }

  // ====== helpers ======
  private mostrarMensaje(texto: string, tipo: MsgType, autoCloseMs?: number) {
    if (this.msgTimer) clearTimeout(this.msgTimer);
    this.mensaje = texto;
    this.msgType = tipo;
    this.cdr.detectChanges();

    if (autoCloseMs) {
      this.msgTimer = setTimeout(() => {
        this.mensaje = '';
        this.msgType = '';
        this.cdr.detectChanges();
      }, autoCloseMs);
    }
  }

  private limpiarMensaje() {
    if (this.msgTimer) clearTimeout(this.msgTimer);
    this.mensaje = '';
    this.msgType = '';
  }

  private getIdUsuarioFromSession(): number | null {
    const user: any = getSessionUser();
    const id = user?.['idUsuario'] ?? user?.['id_usuario'] ?? user?.['id'] ?? null;
    const n = Number(id);
    if (!n || !Number.isFinite(n)) return null;
    return n;
  }

  private errMsg(e: any): string {
    if (typeof e?.error === 'string') return e.error;
    if (typeof e?.error?.message === 'string') return e.error.message;
    if (e?.status === 409) return 'Ya existe una asignación para ese docente en este periodo.';
    if (e?.status === 400) return 'Datos inválidos. Revisa lo que estás enviando.';
    if (e?.status === 0)   return 'No hay conexión con el backend.';
    return e?.message || 'Error desconocido.';
  }
}

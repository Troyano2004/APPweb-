import { ChangeDetectorRef, Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs/operators';
import { AnteproyectoService } from './service';
import { Anteproyecto, AnteproyectoVersionRequest } from './model';
import { getSessionUser } from '../../services/session';

@Component({
  selector: 'app-anteproyecto',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './anteproyecto.html',
  styleUrls: ['./anteproyecto.scss'],
})
export class AnteproyectoComponent {

  fechaTexto = '';
  horaTexto = '';
  private timer: any;

  cargando = false;
  mensaje = '';

  anteproyecto: Anteproyecto | null = null;

  idEstudiante!: number;
  idAnteproyecto!: number;

  nombreEstudiante = '';

  tab: 'portada'|'introduccion'|'marco'|'resultados'|'biblio' = 'portada';

  form: FormGroup;

  constructor(
    private api: AnteproyectoService,
    private fb: FormBuilder,
    private cdr: ChangeDetectorRef
  ) {
    this.form = this.fb.group({
      titulo: ['', Validators.required],
      temaInvestigacion: ['', Validators.required],
      planteamientoProblema: ['', Validators.required],
      objetivosGenerales: ['', Validators.required],
      objetivosEspecificos: ['', Validators.required],
      marcoTeorico: ['', Validators.required],
      metodologia: ['', Validators.required],
      resultadosEsperados: ['', Validators.required],
      bibliografia: ['', Validators.required],
      comentarioCambio: [''],
    });
  }

  get bloqueado(): boolean {
    const st = (this.anteproyecto?.estado || '').toUpperCase();
    return st === 'EN_REVISION' || st === 'APROBADA' || st === 'RECHAZADA';
  }

  get puedeEditar(): boolean {
    return !this.bloqueado && (this.anteproyecto?.estado || '') !== 'NO_DISPONIBLE';
  }

  ngOnInit() {
    this.actualizarFechaHora();
    this.timer = setInterval(() => this.actualizarFechaHora(), 1000);

    const user = getSessionUser();
    const idUsuario = Number(user?.['idUsuario']);
    if (!idUsuario) { this.mensaje = 'No hay idUsuario en sesión'; return; }

    // 👇 en tu modelo, estudianteId = usuarioId (PK=FK)
    this.idEstudiante = idUsuario;

    setTimeout(() => this.cargarMiAnteproyecto(), 0);
  }

  ngOnDestroy() {
    if (this.timer) clearInterval(this.timer);
  }

  // ✅ BOTÓN: recarga SOLO la última versión guardada
  recargarUltimaVersion() {
    if (!this.idAnteproyecto) return;

    this.mensaje = '';
    this.cargando = true;
    this.cdr.detectChanges();

    this.api.ultimaVersion(this.idAnteproyecto)
      .pipe(finalize(() => { this.cargando = false; this.cdr.detectChanges(); }))
      .subscribe({
        next: (v: any) => {
          this.form.patchValue({
            titulo: v.titulo || '',
            temaInvestigacion: v.temaInvestigacion || '',
            planteamientoProblema: v.planteamientoProblema || '',
            objetivosGenerales: v.objetivosGenerales || '',
            objetivosEspecificos: v.objetivosEspecificos || '',
            marcoTeorico: v.marcoTeorico || '',
            metodologia: v.metodologia || '',
            resultadosEsperados: v.resultadosEsperados || '',
            bibliografia: v.bibliografia || '',
            comentarioCambio: '',
          }, { emitEvent: false });

          this.mensaje = `Recargada la última versión guardada (v${v.numeroVersion ?? '-'})`;
        },
        error: () => {
          this.mensaje = 'Aún no hay versiones guardadas para recargar.';
        }
      });
  }

  // ✅ BOTÓN: copiar desde propuesta (snapshot)
  copiarDesdePropuesta() {
    if (!this.anteproyecto?.propuesta) {
      this.mensaje = 'No hay datos de propuesta para copiar.';
      return;
    }

    const p = this.anteproyecto.propuesta;

    this.form.patchValue({
      titulo: p.titulo || '',
      temaInvestigacion: p.temaInvestigacion || '',
      planteamientoProblema: p.planteamientoProblema || '',
      objetivosGenerales: p.objetivoGeneral || '',
      objetivosEspecificos: p.objetivosEspecificos || '',
      metodologia: p.metodologia || '',
      bibliografia: p.bibliografia || '',
      // marco/resultados no vienen en propuesta
      marcoTeorico: this.form.value.marcoTeorico || '',
      resultadosEsperados: this.form.value.resultadosEsperados || '',
      comentarioCambio: '',
    }, { emitEvent: false });

    this.mensaje = 'Se copiaron campos base desde la propuesta aprobada.';
  }

  cargarMiAnteproyecto() {
    this.mensaje = '';
    this.cargando = true;
    this.cdr.detectChanges();

    this.api.miAnteproyecto(this.idEstudiante)
      .pipe(finalize(() => {
        this.cargando = false;
        this.cdr.detectChanges();
      }))
      .subscribe({
        next: (a) => {
          this.anteproyecto = a;

          if (!a || a.estado === 'NO_DISPONIBLE' || a.idAnteproyecto == null) {
            this.form.disable({ emitEvent: false });
            this.nombreEstudiante = '';
            this.mensaje = a?.mensaje || 'No disponible.';
            return;
          }

          this.idAnteproyecto = a.idAnteproyecto;
          localStorage.setItem('est_idAnteproyecto', String(this.idAnteproyecto));
          this.nombreEstudiante = `${a.nombresEstudiante || ''} ${a.apellidosEstudiante || ''}`.trim();

          const uv = a.ultimaVersion;

          if (uv) {
            // ✅ si hay versión, se carga la versión
            this.form.patchValue({
              titulo: uv.titulo || '',
              temaInvestigacion: uv.temaInvestigacion || '',
              planteamientoProblema: uv.planteamientoProblema || '',
              objetivosGenerales: uv.objetivosGenerales || '',
              objetivosEspecificos: uv.objetivosEspecificos || '',
              marcoTeorico: uv.marcoTeorico || '',
              metodologia: uv.metodologia || '',
              resultadosEsperados: uv.resultadosEsperados || '',
              bibliografia: uv.bibliografia || '',
              comentarioCambio: '',
            }, { emitEvent: false });
          } else {
            // ✅ si NO hay versiones, precarga con propuesta (si viene) para que no esté vacío
            this.form.reset({
              titulo: a.propuesta?.titulo || '',
              temaInvestigacion: a.propuesta?.temaInvestigacion || '',
              planteamientoProblema: a.propuesta?.planteamientoProblema || '',
              objetivosGenerales: a.propuesta?.objetivoGeneral || '',
              objetivosEspecificos: a.propuesta?.objetivosEspecificos || '',
              marcoTeorico: '',
              metodologia: a.propuesta?.metodologia || '',
              resultadosEsperados: '',
              bibliografia: a.propuesta?.bibliografia || '',
              comentarioCambio: '',
            }, { emitEvent: false });
          }

          if (this.puedeEditar) this.form.enable({ emitEvent: false });
          else this.form.disable({ emitEvent: false });
        },
        error: (e) => {
          this.mensaje = this.err(e);
          this.form.disable({ emitEvent: false });
        }
      });
  }

  guardarBorrador() {
    if (!this.idAnteproyecto) return;

    if (!this.puedeEditar) {
      this.mensaje = `No puedes continuar: está ${this.anteproyecto?.estado}`;
      return;
    }

    if (this.form.invalid) {
      this.mensaje = 'Completa todos los campos.';
      this.form.markAllAsTouched();
      return;
    }

    const req: AnteproyectoVersionRequest = this.form.getRawValue();

    this.cargando = true;
    this.api.guardarBorrador(this.idAnteproyecto, req)
      .pipe(finalize(() => { this.cargando = false; this.cdr.detectChanges(); }))
      .subscribe({
        next: () => {
          this.mensaje = 'Borrador guardado';
          this.cargarMiAnteproyecto();
        },
        error: (e) => this.mensaje = this.err(e)
      });
  }

  enviarARevision() {
    if (!this.idAnteproyecto) return;

    if (!this.puedeEditar) {
      this.mensaje = `No puedes continuar: está ${this.anteproyecto?.estado}`;
      return;
    }

    if (this.form.invalid) {
      this.mensaje = 'Completa todos los campos.';
      this.form.markAllAsTouched();
      return;
    }

    const req: AnteproyectoVersionRequest = this.form.getRawValue();

    this.cargando = true;
    this.api.enviarRevision(this.idAnteproyecto, req)
      .pipe(finalize(() => { this.cargando = false; this.cdr.detectChanges(); }))
      .subscribe({
        next: () => {
          this.mensaje = 'Enviado a revisión';
          this.cargarMiAnteproyecto();
        },
        error: (e) => this.mensaje = this.err(e)
      });
  }

  private actualizarFechaHora() {
    const now = new Date();
    const dias = ['domingo','lunes','martes','miércoles','jueves','viernes','sábado'];
    const meses = ['enero','febrero','marzo','abril','mayo','junio','julio','agosto','septiembre','octubre','noviembre','diciembre'];
    this.fechaTexto = `${dias[now.getDay()]}, ${now.getDate()} de ${meses[now.getMonth()]} de ${now.getFullYear()}`;

    let h = now.getHours();
    const m = String(now.getMinutes()).padStart(2, '0');
    const s = String(now.getSeconds()).padStart(2, '0');
    const ampm = h >= 12 ? 'p. m.' : 'a. m.';
    h = h % 12; h = h === 0 ? 12 : h;
    this.horaTexto = `${h}:${m}:${s} ${ampm}`;
  }

  private err(e: any) {
    if (typeof e?.error === 'string') return e.error;
    if (typeof e?.error?.message === 'string') return e.error.message;
    return e?.message || 'Error';
  }
}

import { ChangeDetectorRef, Component, OnInit, OnDestroy } from '@angular/core';
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
export class AnteproyectoComponent implements OnInit, OnDestroy {

  fechaTexto = '';
  horaTexto = '';
  private timer: any;

  cargando = false;
  mensaje = '';

  anteproyecto: Anteproyecto | null = null;

  idEstudiante!: number;
  idAnteproyecto!: number;
  ultimaRevision: { decision: string, observacion: string } | null = null;
  mostrarRechazo = false;
  private rechazoTimer: any;

  nombreEstudiante = '';
  tab: 'portada' | 'introduccion' | 'marco' | 'resultados' | 'biblio' = 'portada';
  form: FormGroup;

  // ── IA ──
  iaAnalizando = false;
  iaResultado = '';
  iaSeccion = '';
  iaEstado = ''; // BIEN / NECESITA MEJORAS / INCOMPLETO

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
    if (!idUsuario) {
      this.mensaje = 'No hay idUsuario en sesión';
      return;
    }
    this.idEstudiante = idUsuario;
    setTimeout(() => this.cargarMiAnteproyecto(), 0);
  }

  ngOnDestroy() {
    if (this.timer) clearInterval(this.timer);
    if (this.rechazoTimer) clearTimeout(this.rechazoTimer);
  }

  separarResultaddo(): { titulo: string, texto: string }[] {
    const resultado = this.iaResultado;
    if(!resultado)return [];
    const secciones = [
      { key: 'ESTADO:', titulo: 'Estado' },
      { key: 'OBSERVACIONES:', titulo: 'Observaciones' },
      { key: 'SUGERENCIA PRINCIPAL:', titulo: 'Sugerencia principal' }
    ];
   return secciones.map((sec, index) =>{
      const inicio = resultado.indexOf(sec.key)
      if (inicio ===-1)  return { titulo: sec.titulo, texto: '' };
      const siguiente = secciones[index +1]
      const final = siguiente ? resultado.indexOf(siguiente.key) : resultado.length;
      const contenido =resultado.slice(inicio + sec.key.length , final)
      return {
        titulo: sec.titulo,
        texto: contenido
      };

    }).filter(b =>(
      b.texto.length > 0))

  }



  parsearResultado():
    { titulo: string, texto: string }[]
  {
    if (!this.iaResultado) return [];
    const bloques: { titulo: string, texto: string }[] = [];
    const secciones = [
      { key: 'ESTADO:', titulo: 'Estado' },
      { key: 'OBSERVACIONES:', titulo: 'Observaciones' },
      { key: 'SUGERENCIA PRINCIPAL:', titulo: 'Sugerencia principal' }
    ];

    let texto = this.iaResultado;
    secciones.forEach((sec, i) => {
      const idx = texto.indexOf(sec.key);
      if (idx === -1) return;
      const siguiente = secciones.slice(i + 1).map(s => texto.indexOf(s.key)).filter(x => x > idx);
      const fin = siguiente.length ? Math.min(...siguiente) : texto.length;
      const contenido = texto.substring(idx + sec.key.length, fin).trim();
      if (contenido) bloques.push({ titulo: sec.titulo, texto: contenido });
    });

    return bloques.length ? bloques : [{ titulo: 'Análisis', texto: this.iaResultado }];
  }

  analizarTabActual(): void {
    switch (this.tab) {
      case 'portada':
        const titulo = this.form.get('titulo')?.value || '';
        const tema = this.form.get('temaInvestigacion')?.value || '';
        this.analizarConIa('portada', `TÍTULO: ${titulo}\n\nTEMA: ${tema}`);
        break;
      case 'introduccion':
        const problema = this.form.get('planteamientoProblema')?.value || '';
        const objGen = this.form.get('objetivosGenerales')?.value || '';
        const objEsp = this.form.get('objetivosEspecificos')?.value || '';
        this.analizarConIa('introduccion', `PLANTEAMIENTO: ${problema}\n\nOBJETIVO GENERAL: ${objGen}\n\nOBJETIVOS ESPECÍFICOS: ${objEsp}`);
        break;
      case 'marco':
        this.analizarConIa('marco', this.form.get('marcoTeorico')?.value || '');
        break;
      case 'resultados':
        const metodo = this.form.get('metodologia')?.value || '';
        const resultados = this.form.get('resultadosEsperados')?.value || '';
        this.analizarConIa('resultados', `METODOLOGÍA: ${metodo}\n\nRESULTADOS ESPERADOS: ${resultados}`);
        break;
      case 'biblio':
        this.analizarConIa('bibliografia', this.form.get('bibliografia')?.value || '');
        break;
    }
  }


  nombreTab(): string {
    const nombres: Record<string, string> = {
      portada: 'Portada',
      introduccion: 'Introducción',
      marco: 'Marco Teórico',
      resultados: 'Metodología / Resultados',
      biblio: 'Bibliografía'
    };
    return nombres[this.tab] || this.tab;
  }


  verPdf(): void {
    this.api.generarPdf(this.idEstudiante).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        window.open(url, '_blank');
      },
      error: () => this.mensaje = 'Error al generar el PDF.'
    });
  }
  // ── IA: analizar sección actual ──
  analizarConIa(seccion: string, contenido: string): void {
    if (!contenido.trim()) {
      this.iaResultado = 'Escribe algo antes de analizar.';
      this.iaSeccion = seccion;
      this.iaEstado = '';
      return;
    }
    this.iaAnalizando = true;
    this.iaResultado = '';
    this.iaSeccion = this.nombreTab();
    this.iaEstado = '';


    this.api.analizarSeccion({seccion: seccion,
      contenido: contenido,
      idEstudiante: this.idEstudiante})
      .pipe(finalize(() => { this.iaAnalizando = false; this.cdr.detectChanges(); }))
      .subscribe({
        next: (res) => {
          this.iaResultado = res.resultado;
          const match = res.resultado.match(/ESTADO:\s*(BIEN|NECESITA MEJORAS|INCOMPLETO)/i);
          this.iaEstado = match ? match[1].toUpperCase() : '';
          this.cdr.detectChanges();
        },
        error: () => {
          this.iaResultado = 'Error al conectar con la IA. Intenta de nuevo.';
          this.cdr.detectChanges();
        }
      });
  }
  limpiarIa(): void {
    this.iaResultado = '';
    this.iaSeccion = '';
    this.iaEstado = '';
  }

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
            titulo: v.titulo || '', temaInvestigacion: v.temaInvestigacion || '',
            planteamientoProblema: v.planteamientoProblema || '',
            objetivosGenerales: v.objetivosGenerales || '',
            objetivosEspecificos: v.objetivosEspecificos || '',
            marcoTeorico: v.marcoTeorico || '', metodologia: v.metodologia || '',
            resultadosEsperados: v.resultadosEsperados || '',
            bibliografia: v.bibliografia || '', comentarioCambio: '',
          }, { emitEvent: false });
          this.mensaje = `Recargada la última versión guardada (v${v.numeroVersion ?? '-'})`;
        },
        error: () => { this.mensaje = 'Aún no hay versiones guardadas para recargar.'; }
      });
  }

  copiarDesdePropuesta() {
    if (!this.anteproyecto?.propuesta) { this.mensaje = 'No hay datos de propuesta para copiar.'; return; }
    const p = this.anteproyecto.propuesta;
    this.form.patchValue({
      titulo: p.titulo || '', temaInvestigacion: p.temaInvestigacion || '',
      planteamientoProblema: p.planteamientoProblema || '',
      objetivosGenerales: p.objetivoGeneral || '',
      objetivosEspecificos: p.objetivosEspecificos || '',
      metodologia: p.metodologia || '', bibliografia: p.bibliografia || '',
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
      .pipe(finalize(() => { this.cargando = false; this.cdr.detectChanges(); }))
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
            this.form.patchValue({
              titulo: uv.titulo || '', temaInvestigacion: uv.temaInvestigacion || '',
              planteamientoProblema: uv.planteamientoProblema || '',
              objetivosGenerales: uv.objetivosGenerales || '',
              objetivosEspecificos: uv.objetivosEspecificos || '',
              marcoTeorico: uv.marcoTeorico || '', metodologia: uv.metodologia || '',
              resultadosEsperados: uv.resultadosEsperados || '',
              bibliografia: uv.bibliografia || '', comentarioCambio: '',
            }, { emitEvent: false });
          } else {
            this.form.reset({
              titulo: a.propuesta?.titulo || '',
              temaInvestigacion: a.propuesta?.temaInvestigacion || '',
              planteamientoProblema: a.propuesta?.planteamientoProblema || '',
              objetivosGenerales: a.propuesta?.objetivoGeneral || '',
              objetivosEspecificos: a.propuesta?.objetivosEspecificos || '',
              marcoTeorico: '', metodologia: a.propuesta?.metodologia || '',
              resultadosEsperados: '', bibliografia: a.propuesta?.bibliografia || '',
              comentarioCambio: '',
            }, { emitEvent: false });
          }
          if (this.puedeEditar) this.form.enable({ emitEvent: false });
          else this.form.disable({ emitEvent: false });
          if (a.ultimaVersion?.estadoVersion?.toUpperCase() === 'RECHAZADO') {
            this.activarMensajeRechazo();
            this.api.ultimaRevision(a.idAnteproyecto!).subscribe({
              next: r => { this.ultimaRevision = r; this.cdr.detectChanges(); },
              error: () => {}
            });
          } else {
            this.mostrarRechazo = false;
          }
        },
        error: (e) => { this.mensaje = this.err(e); this.form.disable({ emitEvent: false }); }
      });
  }

  guardarBorrador() {
    if (!this.idAnteproyecto) return;
    if (!this.puedeEditar) { this.mensaje = `No puedes continuar: está ${this.anteproyecto?.estado}`; return; }
    if (this.form.invalid) { this.mensaje = 'Completa todos los campos.'; this.form.markAllAsTouched(); return; }
    const req: AnteproyectoVersionRequest = this.form.getRawValue();
    this.cargando = true;
    this.api.guardarBorrador(this.idAnteproyecto, req)
      .pipe(finalize(() => { this.cargando = false; this.cdr.detectChanges(); }))
      .subscribe({
        next: () => { this.mensaje = 'Borrador guardado'; this.cargarMiAnteproyecto(); },
        error: (e) => this.mensaje = this.err(e)
      });
  }

  enviarARevision() {
    if (!this.idAnteproyecto) return;
    if (!this.puedeEditar) { this.mensaje = `No puedes continuar: está ${this.anteproyecto?.estado}`; return; }
    if (this.form.invalid) { this.mensaje = 'Completa todos los campos.'; this.form.markAllAsTouched(); return; }
    const req: AnteproyectoVersionRequest = this.form.getRawValue();
    this.cargando = true;
    this.api.enviarRevision(this.idAnteproyecto, req)
      .pipe(finalize(() => { this.cargando = false; this.cdr.detectChanges(); }))
      .subscribe({
        next: () => { this.mensaje = 'Enviado a revisión'; this.cargarMiAnteproyecto(); },
        error: (e) => this.mensaje = this.err(e)
      });
  }

  private activarMensajeRechazo() {
    this.mostrarRechazo = true;
    if (this.rechazoTimer) clearTimeout(this.rechazoTimer);
    this.rechazoTimer = setTimeout(() => { this.mostrarRechazo = false; this.cdr.detectChanges(); }, 10000);
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

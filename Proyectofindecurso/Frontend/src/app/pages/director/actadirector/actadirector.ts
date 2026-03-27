import { ChangeDetectorRef, Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs/operators';
import { DirectorApiService } from '../service';
import { ActaRevisionDirectorRequest } from '../model';
import { getSessionUser } from '../../../services/session';
import { Router } from '@angular/router';
import { ViewChild, ElementRef } from '@angular/core';
import pdfMake from 'pdfmake/build/pdfmake';
import * as pdfFonts from 'pdfmake/build/vfs_fonts';
(pdfMake as any).vfs = (pdfFonts as any).default?.pdfMake?.vfs ?? (pdfFonts as any).pdfMake?.vfs


import jsPDF from 'jspdf';
import html2canvas from 'html2canvas'
import {content} from 'html2canvas/dist/types/css/property-descriptors/content';

@Component({
  selector: 'app-actadirector',
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './actadirector.html',
  styleUrl: './actadirector.scss',
})
export class Actadirector {
  cargando = false;
  mensaje = '';

  idDocente!: number;
  idTutoria!: number;
  @ViewChild('paper') paperRef!: ElementRef;
  form: FormGroup;

  constructor(
    private api: DirectorApiService,
    private fb: FormBuilder,
    private cdr: ChangeDetectorRef,
    private router: Router
  ) {
    this.form = this.fb.group({
      directorCargo: ['Director del Proyecto de Investigación', [Validators.required, Validators.maxLength(200)]],
      directorFirma: ['', [Validators.maxLength(200)]],

      estudianteCargo: ['Autor del Proyecto de Investigación', [Validators.required, Validators.maxLength(200)]],
      estudianteFirma: ['', [Validators.maxLength(200)]],

      tituloProyecto: ['', [Validators.required, Validators.minLength(5), Validators.maxLength(600)]],
      objetivo: ['', [Validators.required, Validators.minLength(10), Validators.maxLength(5000)]],
      detalleRevision: ['', [Validators.required, Validators.minLength(10), Validators.maxLength(8000)]],
      observaciones: ['', [Validators.maxLength(8000)]],
      cumplimiento: ['COMPLETO', [Validators.required]],
      conclusion: ['', [Validators.required, Validators.minLength(10), Validators.maxLength(8000)]],
    });
  }

  ngOnInit() {
    const user = getSessionUser();
    const idUsuario = Number(user?.['idUsuario']);
    if (!idUsuario) { this.mensaje = 'No hay idUsuario en sesión'; return; }
    this.idDocente = idUsuario;

    const raw = localStorage.getItem('director_idTutoria');
    this.idTutoria = Number(raw || 0);
    if (!this.idTutoria) { this.mensaje = 'No hay tutoría seleccionada'; return; }
    const titulo = localStorage.getItem('director_tituloProyecto') || '';

    const nombre = localStorage.getItem('director_estudianteNombre') || ''

    this.form.patchValue({
      tituloProyecto: titulo

    }, { emitEvent: false });

    this.cargarSiExiste();
  }

  cargarSiExiste() {
    this.cargando = true;
    this.mensaje = '';
    this.api.obtenerActa(this.idTutoria, this.idDocente)
      .pipe(finalize(() => { this.cargando = false; this.cdr.detectChanges(); }))
      .subscribe({
        next: (a) => {
          this.form.patchValue(a, { emitEvent:false });
          this.mensaje = 'Acta cargada (puedes editar y guardar).';
        },
        error: () => {
          this.mensaje = 'Aún no existe acta. Llena y guarda.';
        }
      });
  }

  guardar() {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.mensaje = 'Completa los campos obligatorios.';
      return;
    }

    const req: ActaRevisionDirectorRequest = this.form.getRawValue();

    this.cargando = true;
    this.api.guardarActa(this.idTutoria, this.idDocente, req)
      .pipe(finalize(() => { this.cargando = false; this.cdr.detectChanges(); }))
      .subscribe({
        next: () => this.mensaje = 'Acta guardada. Tutoría marcada como REALIZADA.',
        error: (e) => this.mensaje = this.err(e),
      });
  }
  imprimir() {
    const f = this.form.getRawValue();

    const verde = '#0f7a3a';
    const grisSeccion = '#f7f7f7';
    const grisSub = '#f2f2f2';
    const borde = '#444';

    const docDefinition: any = {
      pageSize: 'A4',
      pageMargins: [40, 50, 40, 50],
      defaultStyle: {
        fontSize: 11,
        lineHeight: 1.2
      },

      content: [

        // TÍTULO
        {
          text: 'Universidad Técnica Estatal de Quevedo', fontSize: 27, alignment: 'center', color: '#6b7280', margin: [0, 0, 0, 10]
        },
        {
          text: 'ACTA DE REVISIÓN - DIRECTOR', fontSize: 20, bold: true, alignment: 'center', color: verde, margin: [0, 0, 0, 8]
        },


        // TABLA PRINCIPAL
        {
          table: {
            widths: ['36%', '34%', '30%'],
            body: [

              // ENCABEZADO PARTICIPANTES
              [
                {
                  text: 'Participantes', bold: true, alignment: 'center', fillColor: verde, color: '#fff', fontSize: 11
                },
                {
                  text: 'Cargo', bold: true, alignment: 'center', fillColor: verde, color: '#fff', fontSize: 11
                },
                {
                  text: 'Firmas', bold: true, alignment: 'center', fillColor: verde, color: '#fff', fontSize: 11
                }
              ],

              // DIRECTOR
              [
                {
                  text: f.directorNombre || 'Director', fontSize: 11, margin: [0, 6, 0, 6]
                },
                {
                  text: f.directorCargo || '—', fontSize: 11, margin: [0, 6, 0, 6]
                },
                {
                  text: f.directorFirma || '—', alignment: 'center', fontSize: 11, margin: [0, 6, 0, 6]
                }
              ],

              // ESTUDIANTE
              [
                {
                  text: f.estudianteNombre || 'Estudiante', fontSize: 11, margin: [0, 6, 0, 6]
                },
                {
                  text: f.estudianteCargo || '—', fontSize: 11, margin: [0, 6, 0, 6]
                },
                {
                  text: f.estudianteFirma || '—', alignment: 'center', fontSize: 11, margin: [0, 6, 0, 6]
                }
              ],

              // TÍTULO PROYECTO
              [
                {
                  text: 'Proyecto de Integración Curricular:', colSpan: 3, bold: true, fontSize: 11, fillColor: grisSeccion, margin: [0, 2, 0, 2]
                },
                {},
                {}
              ],

              // VALOR PROYECTO
              [
                {
                  text: f.tituloProyecto || '—', colSpan: 3, fontSize: 11, margin: [0, 2, 0, 2]
                },
                {},
                {}
              ],

              // PROCESO DE REVISIÓN
              [
                {
                  text: 'Proceso de revisión:', colSpan: 3, bold: true, fontSize: 11, fillColor: grisSeccion, margin: [0, 2, 0, 2]
                },
                {},
                {}
              ],

              // ENCABEZADO REVISIÓN
              [
                {
                  text: 'Objetivo', bold: true, fontSize: 11, fillColor: grisSub, margin: [0, 2, 0, 2]
                },
                {
                  text: 'Detalle de la revisión', bold: true, fontSize: 11, fillColor: grisSub, margin: [0, 2, 0, 2]
                },
                {
                  text: 'Nivel de Cumplimiento', bold: true, fontSize: 11, fillColor: grisSub, margin: [0, 2, 0, 2]
                }
              ],

              // DATOS REVISIÓN
              [
                {
                  text: f.objetivo || '—', alignment: 'justify', fontSize: 11, margin: [0, 6, 0, 6]
                },
                {
                  text: f.detalleRevision || '—', alignment: 'justify', fontSize: 11, margin: [0, 6, 0, 6]
                },
                {
                  text: f.cumplimiento || '—',
                  fontSize: 11,
                  margin: [0, 6, 0, 6]
                }
              ],

              // OBSERVACIONES
              [
                {
                  text: 'Observaciones:',
                  colSpan: 3,
                  bold: true,
                  fontSize: 11,
                  fillColor: grisSeccion,
                  margin: [0, 2, 0, 2]
                },
                {},
                {}
              ],
              [
                {
                  text: f.observaciones || 'Sin observaciones.', colSpan: 3, fontSize: 11, alignment: 'justify', margin: [0, 6, 0, 6]
                },
                {},
                {}
              ],

              // CONCLUSIÓN
              [
                {
                  text: [
                    { text: 'Conclusión: ', bold: true },
                    { text: f.conclusion || '—' }
                  ],
                  colSpan: 3, fontSize: 11, alignment: 'justify', margin: [0, 6, 0, 6]
                },
                {},
                {}
              ]
            ]
          },

          layout: {

            vLineWidth: () => 0.8,
            hLineColor: () => borde,
            vLineColor: () => borde,
            paddingLeft: () => 12,
            paddingRight: () => 12,
            paddingTop: () => 10,
            paddingBottom: () => 10
          }
        },

        // FOOTER
        {
          text: 'Documento generado desde el Sistema de Titulación UTEQ',
          fontSize: 8,
          color: '#9ca3af',
          alignment: 'center',
          margin: [0, 16, 0, 0]
        }
      ]
    };

    pdfMake.createPdf(docDefinition).download('acta-revision.pdf');
  }
  volver() { this.router.navigate(['/app/director/tutorias']); }

  private err(e:any){
    if (typeof e?.error === 'string') return e.error;
    if (typeof e?.error?.message === 'string') return e.error.message;
    return e?.message || 'Error';
  }
}

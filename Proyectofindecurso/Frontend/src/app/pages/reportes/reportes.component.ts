import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ReporteService } from './service';
import { PeriodoSelect, EstudianteSelect } from './model';

@Component({
  selector: 'app-reportes',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './reportes.component.html',
  styleUrls: ['./reportes.component.scss']
})
export class ReportesComponent implements OnInit {
  periodos: PeriodoSelect[] = [];
  estudiantes: EstudianteSelect[] = [];
  periodoSeleccionado?: number;
  estudianteSeleccionado?: number;
  proyectoIdActas?: number;

  constructor(private svc: ReporteService) {}

  ngOnInit() {
    this.svc.getPeriodos().subscribe(p => this.periodos = p);
    this.svc.getEstudiantes().subscribe(e => this.estudiantes = e);
  }

  expedientePdf()   { if (this.estudianteSeleccionado) this.svc.expedientePdf(this.estudianteSeleccionado); }
  expedienteExcel() { if (this.estudianteSeleccionado) this.svc.expedienteExcel(this.estudianteSeleccionado); }
  periodoPdf()      { if (this.periodoSeleccionado)    this.svc.periodoPdf(this.periodoSeleccionado); }
  periodoExcel()    { if (this.periodoSeleccionado)    this.svc.periodoExcel(this.periodoSeleccionado); }
  actasTutoria()    { if (this.proyectoIdActas)        this.svc.actasTutoriaPdf(this.proyectoIdActas); }
  actasSustentacion(){ if (this.proyectoIdActas)       this.svc.actasSustentacionPdf(this.proyectoIdActas); }

  nombreEstudiante(e: EstudianteSelect): string {
    return e.usuario.nombres + ' ' + e.usuario.apellidos + ' - ' + e.usuario.cedula;
  }
}

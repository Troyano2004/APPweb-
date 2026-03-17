import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ReporteService } from '../service';
import { EstudianteSelect } from '../model';

@Component({
  selector: 'app-reporte-expediente',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './expediente.component.html',
  styleUrls: ['./expediente.component.scss']
})
export class ExpedienteComponent implements OnInit {
  private svc = inject(ReporteService);
  estudiantes: EstudianteSelect[] = [];
  estudianteSeleccionado?: number;
  cargando = true;

  ngOnInit(): void {
    this.svc.getEstudiantes().subscribe({
      next: (data) => { this.estudiantes = data; this.cargando = false; },
      error: () => { this.cargando = false; }
    });
  }

  expedientePdf()   { if (this.estudianteSeleccionado) this.svc.expedientePdf(this.estudianteSeleccionado); }
  expedienteExcel() { if (this.estudianteSeleccionado) this.svc.expedienteExcel(this.estudianteSeleccionado); }

  nombreEstudiante(e: EstudianteSelect): string {
    return e.usuario.nombres + ' ' + e.usuario.apellidos + ' - ' + e.usuario.cedula;
  }
}

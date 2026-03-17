import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ReporteService } from '../service';
import { PeriodoSelect } from '../model';

@Component({
  selector: 'app-reporte-periodo',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './periodo.component.html',
  styleUrls: ['./periodo.component.scss']
})
export class ReportePeriodoComponent implements OnInit {
  private svc = inject(ReporteService);
  periodos: PeriodoSelect[] = [];
  periodoSeleccionado?: number;
  estadoFiltro = '';
  cargando = true;
  estados = ['ANTEPROYECTO', 'DESARROLLO', 'PREDEFENSA', 'DEFENSA', 'FINALIZADO'];

  ngOnInit(): void {
    this.svc.getPeriodos().subscribe({
      next: (data) => { this.periodos = data; this.cargando = false; },
      error: () => { this.cargando = false; }
    });
  }

  periodoPdf() {
    if (!this.periodoSeleccionado) return;
    let url = `http://localhost:8080/api/reportes/periodo/${this.periodoSeleccionado}/pdf`;
    if (this.estadoFiltro) url += `?estado=${this.estadoFiltro}`;
    this.svc.descargar(url, `Periodo_${this.periodoSeleccionado}.pdf`);
  }

  periodoExcel() {
    if (!this.periodoSeleccionado) return;
    let url = `http://localhost:8080/api/reportes/periodo/${this.periodoSeleccionado}/excel`;
    if (this.estadoFiltro) url += `?estado=${this.estadoFiltro}`;
    this.svc.descargar(url, `Periodo_${this.periodoSeleccionado}.xlsx`);
  }
}

import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ReporteService } from '../service';

@Component({
  selector: 'app-reporte-actas',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './actas.component.html',
  styleUrls: ['./actas.component.scss']
})
export class ActasComponent {
  private svc = inject(ReporteService);
  proyectoIdActas?: number;

  actasTutoria()     { if (this.proyectoIdActas) this.svc.actasTutoriaPdf(this.proyectoIdActas); }
  actasSustentacion(){ if (this.proyectoIdActas) this.svc.actasSustentacionPdf(this.proyectoIdActas); }
}

import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';

interface ReporteConfig {
  id: number;
  clave: string;
  valor: string;
  descripcion: string;
  tipo: string;
}

@Component({
  selector: 'app-parametros',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './parametros.component.html',
  styleUrls: ['./parametros.component.scss']
})
export class ParametrosComponent implements OnInit {
  configs: ReporteConfig[] = [];
  guardando: { [id: number]: boolean } = {};
  mensaje: string = '';

  get secciones()  { return this.configs.filter(c => c.clave.startsWith('seccion_')); }
  get filtros()    { return this.configs.filter(c => c.clave.startsWith('filtro_')); }
  get firma()      { return this.configs.filter(c => c.clave.startsWith('firma_')); }

  private http = inject(HttpClient);

  constructor() {}

  ngOnInit() {
    this.http.get<ReporteConfig[]>('http://localhost:8080/api/reporte-config')
      .subscribe({
        next: (data) => {
          console.log('Configs cargadas:', data);
          this.configs = data;
        },
        error: (err) => console.error('Error cargando configs:', err)
      });
  }

  guardar(config: ReporteConfig) {
    this.guardando[config.id] = true;
    this.http.put(`http://localhost:8080/api/reporte-config/${config.id}`,
      { valor: config.valor }).subscribe({
      next: () => {
        this.guardando[config.id] = false;
        this.mensaje = 'Guardado correctamente';
        setTimeout(() => this.mensaje = '', 3000);
      },
      error: () => { this.guardando[config.id] = false; }
    });
  }

  toggleBoolean(config: ReporteConfig) {
    config.valor = config.valor === 'true' ? 'false' : 'true';
    this.guardar(config);
  }
}

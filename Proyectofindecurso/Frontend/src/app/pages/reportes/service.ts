import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { PeriodoSelect, EstudianteSelect } from './model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class ReporteService {
  private base = `${environment.apiUrl}/api/reportes`;
  constructor(private http: HttpClient) {}

  getPeriodos(): Observable<PeriodoSelect[]> {
    return this.http.get<PeriodoSelect[]>(`${this.base}/periodos`, { withCredentials: true });
  }
  getEstudiantes(): Observable<EstudianteSelect[]> {
    return this.http.get<EstudianteSelect[]>(`${this.base}/estudiantes`, { withCredentials: true });
  }
  descargar(url: string, filename: string) {
    this.http.get(url, { responseType: 'blob', withCredentials: true }).subscribe(blob => {
      const a = document.createElement('a');
      a.href = URL.createObjectURL(blob);
      a.download = filename;
      a.click();
      URL.revokeObjectURL(a.href);
    });
  }
  expedientePdf(id: number)  { this.descargar(`${this.base}/expediente/${id}/pdf`,  `Expediente_${id}.pdf`);  }
  expedienteExcel(id: number){ this.descargar(`${this.base}/expediente/${id}/excel`,`Expediente_${id}.xlsx`); }
  periodoPdf(id: number, idCarrera?: number, estado?: string) {
    let url = `${this.base}/periodo/${id}/pdf`;
    const params: string[] = [];
    if (idCarrera) params.push(`idCarrera=${idCarrera}`);
    if (estado)    params.push(`estado=${estado}`);
    if (params.length) url += '?' + params.join('&');
    this.descargar(url, `Periodo_${id}.pdf`);
  }
  periodoExcel(id: number)   { this.descargar(`${this.base}/periodo/${id}/excel`,    `Periodo_${id}.xlsx`);    }
  actasTutoriaPdf(id: number){ this.descargar(`${this.base}/actas/tutoria/${id}/pdf`,`Actas_Tutorias_${id}.pdf`);}
  actasSustentacionPdf(id: number){ this.descargar(`${this.base}/actas/sustentacion/${id}/pdf`,`Actas_Sustentacion_${id}.pdf`);}
}

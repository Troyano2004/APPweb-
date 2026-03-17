import {Injectable} from '@angular/core';
import {ZoomConfigDto, ZoomConfigRequest} from './model';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {map} from 'rxjs/operators';

@Injectable({providedIn:'root'})
export class  ZoomConfigService {
  private readonly base = 'http://localhost:8080/api/docente/zoom-config';

  constructor(private http:HttpClient) {
  }
  obtener(idDocente: number): Observable<ZoomConfigDto> {
    return this.http.get<ZoomConfigDto>(`${this.base}/${idDocente}`);
  }

  guardar(idDocente: number, req: ZoomConfigRequest): Observable<ZoomConfigDto> {
    return this.http.post<ZoomConfigDto>(`${this.base}/${idDocente}`, req);
  }

  eliminar(idDocente: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/${idDocente}`);
  }
  verificarConfiguracion(idDocente: number): Observable<boolean> {
    return this.obtener(idDocente).pipe(
      map(config => config.configurado)
    );
  }
}

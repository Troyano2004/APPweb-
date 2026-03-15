
import {Injectable} from '@angular/core';
import {SesionActivaDto} from './model';
import {HttpClient} from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class SesionesActivasService {
  private readonly base = 'http://localhost:8080/api/admin/sesiones';

  constructor(private http: HttpClient) {
  }
    listarActivas():Observable<SesionActivaDto[]>{
      return this.http.get<SesionActivaDto[]>(this.base);
    }
    cerrarSesion(id: number): Observable<any> {
    return this.http.delete(`${this.base}/${id}`);
  }
  }


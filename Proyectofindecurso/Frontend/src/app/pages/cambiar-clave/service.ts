import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CambiarClaveRequest } from './model';

@Injectable({ providedIn: 'root' })
export class CambiarClaveService {

  private readonly base = 'http://localhost:8080/api/auth';

  constructor(private http: HttpClient) {}

  cambiarClave(req: CambiarClaveRequest): Observable<void> {
    return this.http.post<void>(`${this.base}/cambiar-clave`, req);
  }
}

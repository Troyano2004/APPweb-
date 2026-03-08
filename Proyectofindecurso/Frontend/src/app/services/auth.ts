import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly baseUrl = 'http://localhost:8080/api/auth';

  constructor(private http: HttpClient) {}

  login(usuarioLogin: string, password: string) {
    // ✅ FIX: withCredentials envia la cookie JSESSIONID en cada request
    // Sin esto, Angular no adjunta la cookie y el backend ve una sesión
    // nueva vacía en cada request → siempre usa auth_reader (DEFAULT)
    return this.http.post(
      `${this.baseUrl}/login`,
      { usuarioLogin, password },
      { withCredentials: true }
    );
  }

  logout() {
    return this.http.post(
      `${this.baseUrl}/logout`,
      {},
      { withCredentials: true }
    );
  }
}

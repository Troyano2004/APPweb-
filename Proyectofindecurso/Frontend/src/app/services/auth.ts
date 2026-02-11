import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly baseUrl = 'http://localhost:8080/api/auth';

  constructor(private http: HttpClient) {}

  login(usuarioLogin: string, password: string) {
    return this.http.post(`${this.baseUrl}/login`, { usuarioLogin, password });
  }
}

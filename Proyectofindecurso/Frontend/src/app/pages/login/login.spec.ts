import { ComponentFixture, TestBed } from '@angular/core/testing';
import { LoginComponent } from './login';
import { provideHttpClient } from '@angular/common/http'; // Importante para que no falle el servicio
import { provideHttpClientTesting } from '@angular/common/http/testing';
import {AuthService} from '../../services/auth'; // Para simular peticiones


describe('LoginComponent', () => {
  let component: LoginComponent;
  let fixture: ComponentFixture<LoginComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LoginComponent],
      providers: [
        AuthService, // Proveemos el servicio
        provideHttpClient(), // Necesario porque AuthService lo usa
        provideHttpClientTesting() // Herramienta para que el test no haga peticiones reales
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(LoginComponent);
    component = fixture.componentInstance;
    fixture.detectChanges(); // Dispara la detección de cambios inicial
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

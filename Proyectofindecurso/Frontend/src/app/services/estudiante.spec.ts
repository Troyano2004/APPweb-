import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { Estudiantes } from '../pages/estudiantes/estudiantes';
// Prueba con esta ruta (sube de 'estudiantes' a 'pages', y de 'pages' a 'app')
// Prueba con esta ruta
import { EstudianteService } from '../services/estudiante';

describe('Estudiantes Component', () => {
  let component: Estudiantes;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      // Importamos HttpClientTestingModule para que el servicio de estudiantes no falle al pedir datos
      imports: [HttpClientTestingModule, Estudiantes],
      // Proveemos el servicio que usa tu componente
      providers: [EstudianteService]
    }).compileComponents();

    // Creamos la instancia del componente directamente ya que es Standalone
    const fixture = TestBed.createComponent(Estudiantes);
    component = fixture.componentInstance;
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });
});

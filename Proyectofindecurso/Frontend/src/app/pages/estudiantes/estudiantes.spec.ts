import { ComponentFixture, TestBed } from '@angular/core/testing';
// CORRECCIÓN 1: Importamos 'EstudiantesComponent' (asegúrate que así se llame en estudiantes.ts)
import { EstudiantesComponent } from './estudiantes';
import { EstudianteService } from '../../services/estudiante';
import { of } from 'rxjs';

describe('EstudiantesComponent', () => {
  let component: EstudiantesComponent;
  let fixture: ComponentFixture<EstudiantesComponent>;

  // CORRECCIÓN 2: Creamos el mock manualmente sin usar 'jasmine' para evitar el error TS2304.
  // Esto hace lo mismo que createSpyObj: crea un objeto con la función getEstudiantes.
  const estudianteServiceMock = {
    getEstudiantes: () => of([]) // Devuelve un array vacío observable
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EstudiantesComponent],
      providers: [
        // Le decimos a Angular que use nuestro mock manual
        { provide: EstudianteService, useValue: estudianteServiceMock }
      ]
    })
      .compileComponents();

    fixture = TestBed.createComponent(EstudiantesComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

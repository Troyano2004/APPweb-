import { ComponentFixture, TestBed } from '@angular/core/testing';

import { RegistroEstudianteComponent } from './registro-estudiante';

describe('RegistroEstudiante', () => {
  let component: RegistroEstudianteComponent;
  let fixture: ComponentFixture<RegistroEstudianteComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RegistroEstudianteComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(RegistroEstudianteComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

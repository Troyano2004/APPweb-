import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Directoranteproyectos } from './directoranteproyectos';

describe('Directoranteproyectos', () => {
  let component: Directoranteproyectos;
  let fixture: ComponentFixture<Directoranteproyectos>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Directoranteproyectos]
    })
    .compileComponents();

    fixture = TestBed.createComponent(Directoranteproyectos);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

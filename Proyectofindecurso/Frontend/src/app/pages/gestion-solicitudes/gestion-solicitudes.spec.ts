import { ComponentFixture, TestBed } from '@angular/core/testing';

import { GestionSolicitudes } from './gestion-solicitudes';

describe('GestionSolicitudes', () => {
  let component: GestionSolicitudes;
  let fixture: ComponentFixture<GestionSolicitudes>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [GestionSolicitudes]
    })
    .compileComponents();

    fixture = TestBed.createComponent(GestionSolicitudes);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

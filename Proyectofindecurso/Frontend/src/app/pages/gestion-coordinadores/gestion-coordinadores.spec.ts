import { ComponentFixture, TestBed } from '@angular/core/testing';

import { GestionCoordinadores } from './gestion-coordinadores';

describe('GestionCoordinadores', () => {
  let component: GestionCoordinadores;
  let fixture: ComponentFixture<GestionCoordinadores>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [GestionCoordinadores]
    })
    .compileComponents();

    fixture = TestBed.createComponent(GestionCoordinadores);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

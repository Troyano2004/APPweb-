import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AsignacionDt1 } from './asignacion-dt1';

describe('AsignacionDt1', () => {
  let component: AsignacionDt1;
  let fixture: ComponentFixture<AsignacionDt1>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AsignacionDt1]
    })
    .compileComponents();

    fixture = TestBed.createComponent(AsignacionDt1);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

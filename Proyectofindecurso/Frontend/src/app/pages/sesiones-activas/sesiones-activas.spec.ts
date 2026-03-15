import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SesionesActivas } from './sesiones-activas';

describe('SesionesActivas', () => {
  let component: SesionesActivas;
  let fixture: ComponentFixture<SesionesActivas>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SesionesActivas]
    })
    .compileComponents();

    fixture = TestBed.createComponent(SesionesActivas);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

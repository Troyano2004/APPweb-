import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Tutoriasdirector } from './tutoriasdirector';

describe('Tutoriasdirector', () => {
  let component: Tutoriasdirector;
  let fixture: ComponentFixture<Tutoriasdirector>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Tutoriasdirector]
    })
    .compileComponents();

    fixture = TestBed.createComponent(Tutoriasdirector);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

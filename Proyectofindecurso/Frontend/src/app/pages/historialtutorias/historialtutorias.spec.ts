import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Historialtutorias } from './historialtutorias';

describe('Historialtutorias', () => {
  let component: Historialtutorias;
  let fixture: ComponentFixture<Historialtutorias>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Historialtutorias]
    })
    .compileComponents();

    fixture = TestBed.createComponent(Historialtutorias);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

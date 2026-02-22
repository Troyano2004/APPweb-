import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AnteproyectoComponent } from './anteproyecto';

describe('Anteproyecto', () => {
  let component: AnteproyectoComponent;
  let fixture: ComponentFixture<AnteproyectoComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AnteproyectoComponent]
    })
      .compileComponents();

    fixture = TestBed.createComponent(AnteproyectoComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

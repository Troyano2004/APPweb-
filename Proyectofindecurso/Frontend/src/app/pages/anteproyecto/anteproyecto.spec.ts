import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Anteproyecto } from './anteproyecto';

describe('Anteproyecto', () => {
  let component: Anteproyecto;
  let fixture: ComponentFixture<Anteproyecto>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Anteproyecto]
    })
    .compileComponents();

    fixture = TestBed.createComponent(Anteproyecto);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

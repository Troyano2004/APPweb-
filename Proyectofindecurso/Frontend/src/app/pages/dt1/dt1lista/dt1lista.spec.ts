import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Dt1lista } from './dt1lista';

describe('Dt1lista', () => {
  let component: Dt1lista;
  let fixture: ComponentFixture<Dt1lista>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Dt1lista]
    })
    .compileComponents();

    fixture = TestBed.createComponent(Dt1lista);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

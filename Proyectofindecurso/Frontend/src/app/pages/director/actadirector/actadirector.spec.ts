import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Actadirector } from './actadirector';

describe('Actadirector', () => {
  let component: Actadirector;
  let fixture: ComponentFixture<Actadirector>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Actadirector]
    })
    .compileComponents();

    fixture = TestBed.createComponent(Actadirector);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

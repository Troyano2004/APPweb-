import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Dt1RevisionComponent } from './dt1revision';

describe('Dt1revision', () => {
  let component: Dt1RevisionComponent;
  let fixture: ComponentFixture<Dt1RevisionComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Dt1RevisionComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(Dt1RevisionComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

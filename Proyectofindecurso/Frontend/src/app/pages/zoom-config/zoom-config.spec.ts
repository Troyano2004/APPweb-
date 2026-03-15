import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ZoomConfig } from './zoom-config';

describe('ZoomConfig', () => {
  let component: ZoomConfig;
  let fixture: ComponentFixture<ZoomConfig>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ZoomConfig]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ZoomConfig);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

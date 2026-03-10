import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ConfiguracionCorreoComponent } from './configuracion-correo';

describe('ConfiguracionCorreo', () => {
  let component: ConfiguracionCorreoComponent;
  let fixture: ComponentFixture<ConfiguracionCorreoComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ConfiguracionCorreoComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ConfiguracionCorreoComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

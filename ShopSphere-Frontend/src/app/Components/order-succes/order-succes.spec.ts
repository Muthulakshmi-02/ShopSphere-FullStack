import { ComponentFixture, TestBed } from '@angular/core/testing';

import { OrderSucces } from './order-succes';

describe('OrderSucces', () => {
  let component: OrderSucces;
  let fixture: ComponentFixture<OrderSucces>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [OrderSucces]
    })
    .compileComponents();

    fixture = TestBed.createComponent(OrderSucces);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

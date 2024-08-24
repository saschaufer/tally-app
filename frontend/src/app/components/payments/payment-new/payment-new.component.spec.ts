import {ComponentFixture, TestBed} from '@angular/core/testing';
import {provideRouter} from "@angular/router";
import {Big} from "big.js";
import {MockProvider} from "ng-mocks";
import {of, throwError} from "rxjs";
import {HttpService} from "../../../services/http.service";

import {PaymentNewComponent} from './payment-new.component';
import SpyObj = jasmine.SpyObj;

describe('PaymentNewComponent', () => {

    let component: PaymentNewComponent;
    let fixture: ComponentFixture<PaymentNewComponent>;

    let httpServiceSpy: SpyObj<HttpService>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [PaymentNewComponent],
            providers: [
                MockProvider(HttpService),
                provideRouter([])
            ]
        })
            .compileComponents();

        fixture = TestBed.createComponent(PaymentNewComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();

        httpServiceSpy = spyOnAllFunctions(TestBed.inject(HttpService));
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    it('should create the payment', () => {

        httpServiceSpy.postCreatePayment.and.callFake(() => of(undefined));

        component.newPaymentForm.setValue({
            amount: '123.45'
        });

        component.onSubmit();

        expect(httpServiceSpy.postCreatePayment).toHaveBeenCalledOnceWith(Big('123.45'));
    });

    it('should not create the payment (amount wrong)', () => {

        httpServiceSpy.postCreatePayment.and.callFake(() => of(undefined));

        component.newPaymentForm.controls.amount.setErrors(['wrong']);

        component.onSubmit();

        expect(httpServiceSpy.postCreatePayment).not.toHaveBeenCalled();
    });

    it('should not create the payment (create payment failed)', () => {

        httpServiceSpy.postCreatePayment.and.callFake(() =>
            throwError(() => 'Error on create payment')
        );

        component.newPaymentForm.controls.amount.patchValue('123.45');

        component.onSubmit();

        expect(httpServiceSpy.postCreatePayment).toHaveBeenCalledOnceWith(Big('123.45'));
    });
});

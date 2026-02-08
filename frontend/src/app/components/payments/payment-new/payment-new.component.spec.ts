import {ComponentFixture, TestBed} from '@angular/core/testing';
import {provideRouter} from "@angular/router";
import {Mock} from "@vitest/spy";
import {Big} from "big.js";
import {of, throwError} from "rxjs";
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {HttpService} from "../../../services/http.service";
import {PaymentNewComponent} from './payment-new.component';

describe('PaymentNewComponent', () => {

    let component: PaymentNewComponent;
    let fixture: ComponentFixture<PaymentNewComponent>;

    const httpServiceMock = vi.mockObject(HttpService.prototype);

    let dialogSuccessSpyShowModal: Mock<() => void>;
    let dialogSuccessSpyClose: Mock<(returnValue?: string) => void>;
    let dialogErrorSpyShowModal: Mock<() => void>;
    let dialogErrorSpyClose: Mock<(returnValue?: string) => void>;

    beforeEach(async () => {

        vi.resetAllMocks();

        await TestBed.configureTestingModule({
            imports: [PaymentNewComponent],
            providers: [
                provideRouter([]),
                {provide: HttpService, useValue: httpServiceMock}
            ]
        })
            .compileComponents();

        fixture = TestBed.createComponent(PaymentNewComponent);
        component = fixture.componentInstance;

        dialogSuccessSpyShowModal = vi.spyOn(component.dialogSuccess.nativeElement, 'showModal');
        dialogSuccessSpyClose = vi.spyOn(component.dialogSuccess.nativeElement, 'close');
        dialogErrorSpyShowModal = vi.spyOn(component.dialogError.nativeElement, 'showModal');
        dialogErrorSpyClose = vi.spyOn(component.dialogError.nativeElement, 'close');

        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    it('should create the payment', () => {

        const dialog: HTMLDialogElement = component.dialogSuccess.nativeElement;

        httpServiceMock.postCreatePayment.mockReturnValue(of(undefined));

        component.newPaymentForm.setValue({
            amount: '123.45'
        });

        expect(dialogSuccessSpyShowModal).not.toHaveBeenCalled();
        expect(dialogSuccessSpyClose).not.toHaveBeenCalled();
        expect(dialogErrorSpyShowModal).not.toHaveBeenCalled();
        expect(dialogErrorSpyClose).not.toHaveBeenCalled();

        component.onSubmit();

        expect(dialogSuccessSpyShowModal).toHaveBeenCalled();
        expect(dialogSuccessSpyClose).not.toHaveBeenCalled();
        expect(dialogErrorSpyShowModal).not.toHaveBeenCalled();
        expect(dialogErrorSpyClose).not.toHaveBeenCalled();

        expect(httpServiceMock.postCreatePayment).toHaveBeenCalledExactlyOnceWith(Big('123.45'));

        dialog.dispatchEvent(new Event('click'));

        expect(dialogSuccessSpyShowModal).toHaveBeenCalled();
        expect(dialogSuccessSpyClose).toHaveBeenCalled();
        expect(dialogErrorSpyShowModal).not.toHaveBeenCalled();
        expect(dialogErrorSpyClose).not.toHaveBeenCalled();
    });

    it('should not create the payment (amount wrong)', () => {

        const dialog: HTMLDialogElement = component.dialogError.nativeElement;

        httpServiceMock.postCreatePayment.mockReturnValue(of(undefined));

        component.newPaymentForm.controls.amount.setErrors(['wrong']);

        component.onSubmit();

        expect(httpServiceMock.postCreatePayment).not.toHaveBeenCalled();

        dialog.dispatchEvent(new Event('click'));

        expect(dialogSuccessSpyShowModal).not.toHaveBeenCalled();
        expect(dialogSuccessSpyClose).not.toHaveBeenCalled();
        expect(dialogErrorSpyShowModal).not.toHaveBeenCalled();
        expect(dialogErrorSpyClose).not.toHaveBeenCalled();
    });

    it('should not create the payment (create payment failed)', () => {

        const dialog: HTMLDialogElement = component.dialogError.nativeElement;

        httpServiceMock.postCreatePayment.mockReturnValue(
            throwError(() => 'Error on create payment')
        );

        component.newPaymentForm.controls.amount.patchValue('123.45');

        expect(dialogSuccessSpyShowModal).not.toHaveBeenCalled();
        expect(dialogSuccessSpyClose).not.toHaveBeenCalled();
        expect(dialogErrorSpyShowModal).not.toHaveBeenCalled();
        expect(dialogErrorSpyClose).not.toHaveBeenCalled();

        component.onSubmit();

        expect(dialogSuccessSpyShowModal).not.toHaveBeenCalled();
        expect(dialogSuccessSpyClose).not.toHaveBeenCalled();
        expect(dialogErrorSpyShowModal).toHaveBeenCalled();
        expect(dialogErrorSpyClose).not.toHaveBeenCalled();

        expect(httpServiceMock.postCreatePayment).toHaveBeenCalledExactlyOnceWith(Big('123.45'));

        dialog.dispatchEvent(new Event('click'));

        expect(dialogSuccessSpyShowModal).not.toHaveBeenCalled();
        expect(dialogSuccessSpyClose).not.toHaveBeenCalled();
        expect(dialogErrorSpyShowModal).toHaveBeenCalled();
        expect(dialogErrorSpyClose).toHaveBeenCalled();
    });
});

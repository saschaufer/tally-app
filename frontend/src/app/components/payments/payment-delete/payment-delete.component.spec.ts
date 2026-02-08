import {ComponentFixture, TestBed} from '@angular/core/testing';
import {ActivatedRoute, NavigationExtras, provideRouter, Router} from "@angular/router";
import {Mock} from "@vitest/spy";
import {Big} from "big.js";
import {firstValueFrom, of, throwError} from "rxjs";
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {routeName} from "../../../app.routes";
import {HttpService} from "../../../services/http.service";
import {PaymentDeleteComponent} from './payment-delete.component';

describe('PaymentDeleteComponent', () => {

    let component: PaymentDeleteComponent;
    let fixture: ComponentFixture<PaymentDeleteComponent>;

    const httpServiceMock = vi.mockObject(HttpService.prototype);

    let routerNavigateSpy: Mock<(commands: readonly any[], extras?: NavigationExtras) => Promise<boolean>>;
    let dialogSuccessSpyShowModal: Mock<() => void>;
    let dialogSuccessSpyClose: Mock<(returnValue?: string) => void>;
    let dialogErrorSpyShowModal: Mock<() => void>;
    let dialogErrorSpyClose: Mock<(returnValue?: string) => void>;

    beforeEach(async () => {

        vi.resetAllMocks();

        const urlAppend = encodeURIComponent(window.btoa(JSON.stringify({
            id: 1,
            amount: Big('123.45'),
            timestamp: 132
        })));

        await TestBed.configureTestingModule({
            imports: [PaymentDeleteComponent],
            providers: [
                provideRouter([]),
                {provide: HttpService, useValue: httpServiceMock},
                {provide: ActivatedRoute, useValue: {params: of({payment: urlAppend})}}
            ]
        })
            .compileComponents();

        fixture = TestBed.createComponent(PaymentDeleteComponent);
        component = fixture.componentInstance;

        routerNavigateSpy = vi.spyOn(TestBed.inject(Router), 'navigate');
        dialogSuccessSpyShowModal = vi.spyOn(component.dialogSuccess.nativeElement, 'showModal');
        dialogSuccessSpyClose = vi.spyOn(component.dialogSuccess.nativeElement, 'close');
        dialogErrorSpyShowModal = vi.spyOn(component.dialogError.nativeElement, 'showModal');
        dialogErrorSpyClose = vi.spyOn(component.dialogError.nativeElement, 'close');

        fixture.detectChanges();
    });

    it('should create', () => {

        expect(component).toBeTruthy();

        expect(component.payment?.id).toBe(1);
        expect(component.payment?.amount).toEqual(Big('123.45'));
        expect(component.payment?.timestamp).toEqual(132);
    });

    it('should delete the payment and navigate to ' + routeName.payments, () => {

        const dialog: HTMLDialogElement = component.dialogSuccess.nativeElement;

        httpServiceMock.postDeletePayment.mockReturnValue(of(undefined));
        routerNavigateSpy.mockReturnValue(firstValueFrom(of(true)));

        expect(dialogSuccessSpyShowModal).not.toHaveBeenCalled();
        expect(dialogSuccessSpyClose).not.toHaveBeenCalled();
        expect(dialogErrorSpyShowModal).not.toHaveBeenCalled();
        expect(dialogErrorSpyClose).not.toHaveBeenCalled();

        component.onClickDelete();

        expect(httpServiceMock.postDeletePayment).toHaveBeenCalledExactlyOnceWith(1);

        expect(dialogSuccessSpyShowModal).toHaveBeenCalled();
        expect(dialogSuccessSpyClose).not.toHaveBeenCalled();
        expect(dialogErrorSpyShowModal).not.toHaveBeenCalled();
        expect(dialogErrorSpyClose).not.toHaveBeenCalled();

        expect(routerNavigateSpy).not.toHaveBeenCalled();

        dialog.dispatchEvent(new Event('click'));

        expect(routerNavigateSpy).toHaveBeenCalledExactlyOnceWith(['/' + routeName.payments]);

        expect(dialogSuccessSpyShowModal).toHaveBeenCalled();
        expect(dialogSuccessSpyClose).toHaveBeenCalled();
        expect(dialogErrorSpyShowModal).not.toHaveBeenCalled();
        expect(dialogErrorSpyClose).not.toHaveBeenCalled();
    })

    it('should not navigate to ' + routeName.payments + ' (delete payment failed)', () => {

        const dialog: HTMLDialogElement = component.dialogError.nativeElement;

        httpServiceMock.postDeletePayment.mockReturnValue(
            throwError(() => 'Error on delete payment')
        );

        expect(dialogSuccessSpyShowModal).not.toHaveBeenCalled();
        expect(dialogSuccessSpyClose).not.toHaveBeenCalled();
        expect(dialogErrorSpyShowModal).not.toHaveBeenCalled();
        expect(dialogErrorSpyClose).not.toHaveBeenCalled();

        component.onClickDelete();

        expect(httpServiceMock.postDeletePayment).toHaveBeenCalledExactlyOnceWith(1);

        expect(dialogSuccessSpyShowModal).not.toHaveBeenCalled();
        expect(dialogSuccessSpyClose).not.toHaveBeenCalled();
        expect(dialogErrorSpyShowModal).toHaveBeenCalled();
        expect(dialogErrorSpyClose).not.toHaveBeenCalled();

        dialog.dispatchEvent(new Event('click'));

        expect(routerNavigateSpy).not.toHaveBeenCalled();

        expect(dialogSuccessSpyShowModal).not.toHaveBeenCalled();
        expect(dialogSuccessSpyClose).not.toHaveBeenCalled();
        expect(dialogErrorSpyShowModal).toHaveBeenCalled();
        expect(dialogErrorSpyClose).toHaveBeenCalled();
    })
});

import {ComponentFixture, TestBed} from '@angular/core/testing';
import {NavigationExtras, provideRouter, Router} from "@angular/router";
import {Mock} from "@vitest/spy";
import {Big} from "big.js";
import {firstValueFrom, of, throwError} from "rxjs";
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {routeName} from "../../app.routes";
import {HttpService} from "../../services/http.service";
import {GetAccountBalanceResponse} from "../../services/models/GetAccountBalanceResponse";
import {GetPaymentsResponse} from "../../services/models/GetPaymentsResponse";

import {PaymentsComponent} from './payments.component';

describe('PaymentsComponent', () => {

    let component: PaymentsComponent;
    let fixture: ComponentFixture<PaymentsComponent>;

    const httpServiceMock = vi.mockObject(HttpService.prototype);

    let routerNavigateSpy: Mock<(commands: readonly any[], extras?: NavigationExtras) => Promise<boolean>>;
    let dialogErrorReadingPaymentsShowModal: Mock<() => void>;
    let dialogErrorReadingPaymentsClose: Mock<(returnValue?: string) => void>;
    let dialogErrorReadingAccountBalanceShowModal: Mock<() => void>;
    let dialogErrorReadingAccountBalanceClose: Mock<(returnValue?: string) => void>;

    beforeEach(async () => {

        vi.resetAllMocks();

        await TestBed.configureTestingModule({
            imports: [PaymentsComponent],
            providers: [
                provideRouter([]),
                {provide: HttpService, useValue: httpServiceMock}
            ]
        })
            .compileComponents();

        fixture = TestBed.createComponent(PaymentsComponent);
        component = fixture.componentInstance;

        routerNavigateSpy = vi.spyOn(TestBed.inject(Router), 'navigate');
        dialogErrorReadingPaymentsShowModal = vi.spyOn(component.dialogErrorReadingPayments.nativeElement, 'showModal');
        dialogErrorReadingPaymentsClose = vi.spyOn(component.dialogErrorReadingPayments.nativeElement, 'close');
        dialogErrorReadingAccountBalanceShowModal = vi.spyOn(component.dialogErrorReadingAccountBalance.nativeElement, 'showModal');
        dialogErrorReadingAccountBalanceClose = vi.spyOn(component.dialogErrorReadingAccountBalance.nativeElement, 'close');
    });

    it('should create (amount total positive)', () => {

        httpServiceMock.getReadPayments.mockReturnValue(of([
            {id: 1, amount: Big('123.45'), timestamp: 123},
            {id: 2, amount: Big('678.90'), timestamp: 456}
        ] as GetPaymentsResponse[]));

        httpServiceMock.getReadAccountBalance.mockReturnValue(of({
            amountPayments: Big('12.3'),
            amountPurchases: Big('45.6'),
            amountTotal: Big('78.9')
        } as GetAccountBalanceResponse));

        fixture.detectChanges();

        expect(component).toBeTruthy();

        expect(component.payments).toEqual([
            {id: 2, amount: Big('678.90'), timestamp: 456},
            {id: 1, amount: Big('123.45'), timestamp: 123}
        ] as GetPaymentsResponse[]);

        expect(component.accountBalance).toEqual({
            amountPayments: Big('12.3'),
            amountPurchases: Big('45.6'),
            amountTotal: Big('78.9')
        } as GetAccountBalanceResponse);

        expect(component.isNegative).eq(false);

        expect(dialogErrorReadingPaymentsShowModal).not.toHaveBeenCalled();
        expect(dialogErrorReadingPaymentsClose).not.toHaveBeenCalled();
        expect(dialogErrorReadingAccountBalanceShowModal).not.toHaveBeenCalled();
        expect(dialogErrorReadingAccountBalanceClose).not.toHaveBeenCalled();
    });

    it('should create (amount total negative)', () => {

        httpServiceMock.getReadPayments.mockReturnValue(of([
            {id: 1, amount: Big('123.45'), timestamp: 123},
            {id: 2, amount: Big('678.90'), timestamp: 456}
        ] as GetPaymentsResponse[]));

        httpServiceMock.getReadAccountBalance.mockReturnValue(of({
            amountPayments: Big('12.3'),
            amountPurchases: Big('45.6'),
            amountTotal: Big('-78.9')
        } as GetAccountBalanceResponse));

        fixture.detectChanges();

        expect(component).toBeTruthy();

        expect(component.payments).toEqual([
            {id: 2, amount: Big('678.90'), timestamp: 456},
            {id: 1, amount: Big('123.45'), timestamp: 123}
        ] as GetPaymentsResponse[]);

        expect(component.accountBalance).toEqual({
            amountPayments: Big('12.3'),
            amountPurchases: Big('45.6'),
            amountTotal: Big('-78.9')
        } as GetAccountBalanceResponse);

        expect(component.isNegative).eq(true);

        expect(dialogErrorReadingPaymentsShowModal).not.toHaveBeenCalled();
        expect(dialogErrorReadingPaymentsClose).not.toHaveBeenCalled();
        expect(dialogErrorReadingAccountBalanceShowModal).not.toHaveBeenCalled();
        expect(dialogErrorReadingAccountBalanceClose).not.toHaveBeenCalled();
    });

    it('should navigate to ' + routeName.payments_delete, () => {

        routerNavigateSpy.mockReturnValue(firstValueFrom(of(true)));

        component.payments = [
            {id: 1, amount: Big('123.45'), timestamp: 123},
            {id: 2, amount: Big('678.90'), timestamp: 456}
        ] as GetPaymentsResponse[];

        const urlAppend = encodeURIComponent(window.btoa(JSON.stringify({
            id: 1,
            amount: Big('123.45'),
            timestamp: 123
        })));

        component.onClick(0);

        expect(routerNavigateSpy).toHaveBeenCalledExactlyOnceWith(['/' + routeName.payments_delete + '/' + urlAppend]);

        expect(dialogErrorReadingPaymentsShowModal).not.toHaveBeenCalled();
        expect(dialogErrorReadingPaymentsClose).not.toHaveBeenCalled();
        expect(dialogErrorReadingAccountBalanceShowModal).not.toHaveBeenCalled();
        expect(dialogErrorReadingAccountBalanceClose).not.toHaveBeenCalled();
    });

    it('should create (reading payment failed)', () => {

        const dialog: HTMLDialogElement = component.dialogErrorReadingPayments.nativeElement;

        httpServiceMock.getReadPayments.mockReturnValue(
            throwError(() => 'Error on reading payment')
        );

        httpServiceMock.getReadAccountBalance.mockReturnValue(of({
            amountPayments: Big('12.3'),
            amountPurchases: Big('45.6'),
            amountTotal: Big('78.9')
        } as GetAccountBalanceResponse));

        fixture.detectChanges();

        expect(component).toBeTruthy();

        expect(dialogErrorReadingPaymentsShowModal).toHaveBeenCalled();
        expect(dialogErrorReadingPaymentsClose).not.toHaveBeenCalled();
        expect(dialogErrorReadingAccountBalanceShowModal).not.toHaveBeenCalled();
        expect(dialogErrorReadingAccountBalanceClose).not.toHaveBeenCalled();

        dialog.dispatchEvent(new Event('click'));

        expect(component.payments).toEqual([] as GetPaymentsResponse[]);

        expect(component.accountBalance).toEqual({
            amountPayments: Big('12.3'),
            amountPurchases: Big('45.6'),
            amountTotal: Big('78.9')
        } as GetAccountBalanceResponse);

        expect(component.isNegative).eq(false);

        expect(dialogErrorReadingPaymentsShowModal).toHaveBeenCalled();
        expect(dialogErrorReadingPaymentsClose).toHaveBeenCalled();
        expect(dialogErrorReadingAccountBalanceShowModal).not.toHaveBeenCalled();
        expect(dialogErrorReadingAccountBalanceClose).not.toHaveBeenCalled();
    });

    it('should create (reading account balance failed)', () => {

        const dialog: HTMLDialogElement = component.dialogErrorReadingAccountBalance.nativeElement;

        httpServiceMock.getReadPayments.mockReturnValue(of([
            {id: 1, amount: Big('123.45'), timestamp: 123},
            {id: 2, amount: Big('678.90'), timestamp: 456}
        ] as GetPaymentsResponse[]));

        httpServiceMock.getReadAccountBalance.mockReturnValue(
            throwError(() => 'Error on reading payment')
        );

        fixture.detectChanges();

        expect(component).toBeTruthy();

        expect(dialogErrorReadingPaymentsShowModal).not.toHaveBeenCalled();
        expect(dialogErrorReadingPaymentsClose).not.toHaveBeenCalled();
        expect(dialogErrorReadingAccountBalanceShowModal).toHaveBeenCalled();
        expect(dialogErrorReadingAccountBalanceClose).not.toHaveBeenCalled();

        dialog.dispatchEvent(new Event('click'));

        expect(component.payments).toEqual([
            {id: 2, amount: Big('678.90'), timestamp: 456},
            {id: 1, amount: Big('123.45'), timestamp: 123}
        ] as GetPaymentsResponse[]);

        expect(component.accountBalance).eq(undefined);

        expect(component.isNegative).eq(false);

        expect(dialogErrorReadingPaymentsShowModal).not.toHaveBeenCalled();
        expect(dialogErrorReadingPaymentsClose).not.toHaveBeenCalled();
        expect(dialogErrorReadingAccountBalanceShowModal).toHaveBeenCalled();
        expect(dialogErrorReadingAccountBalanceClose).toHaveBeenCalled();
    });
});

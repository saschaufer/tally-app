import {ComponentFixture, TestBed} from '@angular/core/testing';
import {provideRouter, Router} from "@angular/router";
import {Big} from "big.js";
import {MockProvider} from "ng-mocks";
import {firstValueFrom, of} from "rxjs";
import {routeName} from "../../app.routes";
import {HttpService} from "../../services/http.service";
import {GetAccountBalanceResponse} from "../../services/models/GetAccountBalanceResponse";
import {GetPaymentsResponse} from "../../services/models/GetPaymentsResponse";

import {PaymentsComponent} from './payments.component';
import Spy = jasmine.Spy;
import SpyObj = jasmine.SpyObj;

describe('PaymentsComponent', () => {

    let component: PaymentsComponent;
    let fixture: ComponentFixture<PaymentsComponent>;

    let httpServiceSpy: SpyObj<HttpService>;

    let routerNavigateSpy: Spy;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [PaymentsComponent],
            providers: [
                MockProvider(HttpService),
                provideRouter([])
            ]
        })
            .compileComponents();

        fixture = TestBed.createComponent(PaymentsComponent);
        component = fixture.componentInstance;

        httpServiceSpy = spyOnAllFunctions(TestBed.inject(HttpService));

        routerNavigateSpy = spyOn(TestBed.inject(Router), 'navigate');
    });

    it('should create (amount total positive)', () => {

        httpServiceSpy.getReadPayments.and.callFake(() => of([
            {id: 1, amount: Big('123.45'), timestamp: 123},
            {id: 2, amount: Big('678.90'), timestamp: 456}
        ] as GetPaymentsResponse[]));

        httpServiceSpy.getReadAccountBalance.and.callFake(() => of({
            amountPayments: Big('12.3'),
            amountPurchases: Big('45.6'),
            amountTotal: Big('78.9')
        } as GetAccountBalanceResponse));

        fixture.detectChanges();

        expect(component).toBeTruthy();

        expect(component.payments).toEqual([
            {id: 1, amount: Big('123.45'), timestamp: 123},
            {id: 2, amount: Big('678.90'), timestamp: 456}
        ] as GetPaymentsResponse[]);

        expect(component.accountBalance).toEqual({
            amountPayments: Big('12.3'),
            amountPurchases: Big('45.6'),
            amountTotal: Big('78.9')
        } as GetAccountBalanceResponse);

        expect(component.isNegative).toBeFalse();
    });

    it('should create (amount total negative)', () => {

        httpServiceSpy.getReadPayments.and.callFake(() => of([
            {id: 1, amount: Big('123.45'), timestamp: 123},
            {id: 2, amount: Big('678.90'), timestamp: 456}
        ] as GetPaymentsResponse[]));

        httpServiceSpy.getReadAccountBalance.and.callFake(() => of({
            amountPayments: Big('12.3'),
            amountPurchases: Big('45.6'),
            amountTotal: Big('-78.9')
        } as GetAccountBalanceResponse));

        fixture.detectChanges();

        expect(component).toBeTruthy();

        expect(component.payments).toEqual([
            {id: 1, amount: Big('123.45'), timestamp: 123},
            {id: 2, amount: Big('678.90'), timestamp: 456}
        ] as GetPaymentsResponse[]);

        expect(component.accountBalance).toEqual({
            amountPayments: Big('12.3'),
            amountPurchases: Big('45.6'),
            amountTotal: Big('-78.9')
        } as GetAccountBalanceResponse);

        expect(component.isNegative).toBeTrue();
    });

    it('should navigate to ' + routeName.payments_delete, () => {

        routerNavigateSpy.and.callFake(() => firstValueFrom(of(true)));

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

        expect(routerNavigateSpy).toHaveBeenCalledOnceWith(['/' + routeName.payments_delete + '/' + urlAppend]);
    });
});

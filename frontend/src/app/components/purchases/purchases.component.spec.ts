import {ComponentFixture, TestBed} from '@angular/core/testing';
import {NavigationExtras, provideRouter, Router} from "@angular/router";
import {Big} from "big.js";
import {firstValueFrom, of} from "rxjs";
import {beforeEach, describe, expect, it, Mock, vi} from 'vitest';
import {routeName} from "../../app.routes";
import {HttpService} from "../../services/http.service";
import {GetAccountBalanceResponse} from "../../services/models/GetAccountBalanceResponse";
import {GetPurchasesResponse} from "../../services/models/GetPurchasesResponse";

import {PurchasesComponent} from './purchases.component';

describe('PurchasesComponent', () => {

    let component: PurchasesComponent;
    let fixture: ComponentFixture<PurchasesComponent>;

    const httpServiceMock = vi.mockObject(HttpService.prototype);

    let routerNavigateSpy: Mock<(commands: readonly any[], extras?: NavigationExtras) => Promise<boolean>>;

    beforeEach(async () => {

        vi.resetAllMocks();

        await TestBed.configureTestingModule({
            imports: [PurchasesComponent],
            providers: [
                provideRouter([]),
                {provide: HttpService, useValue: httpServiceMock}
            ]
        })
            .compileComponents();

        fixture = TestBed.createComponent(PurchasesComponent);
        component = fixture.componentInstance;

        routerNavigateSpy = vi.spyOn(TestBed.inject(Router), 'navigate');
    });

    it('should create (amount total positive)', () => {

        httpServiceMock.getReadPurchases.mockReturnValue(of([
            {purchaseId: 1, purchaseTimestamp: 123, productName: "product-1", productPrice: Big('123.45')},
            {purchaseId: 2, purchaseTimestamp: 456, productName: "product-2", productPrice: Big('543.21')}
        ] as GetPurchasesResponse[]));

        httpServiceMock.getReadAccountBalance.mockReturnValue(of({
            amountPayments: Big('12.3'),
            amountPurchases: Big('45.6'),
            amountTotal: Big('78.9')
        } as GetAccountBalanceResponse));

        fixture.detectChanges();

        expect(component).toBeTruthy();

        expect(component.purchases).toEqual([
            {purchaseId: 2, purchaseTimestamp: 456, productName: "product-2", productPrice: Big('543.21')},
            {purchaseId: 1, purchaseTimestamp: 123, productName: "product-1", productPrice: Big('123.45')}
        ] as GetPurchasesResponse[]);

        expect(component.accountBalance).toEqual({
            amountPayments: Big('12.3'),
            amountPurchases: Big('45.6'),
            amountTotal: Big('78.9')
        } as GetAccountBalanceResponse);

        expect(component.isNegative).toBeFalsy();
    });

    it('should create (amount total negative)', () => {

        httpServiceMock.getReadPurchases.mockReturnValue(of([
            {purchaseId: 1, purchaseTimestamp: 123, productName: "product-1", productPrice: Big('123.45')},
            {purchaseId: 2, purchaseTimestamp: 456, productName: "product-2", productPrice: Big('543.21')}
        ] as GetPurchasesResponse[]));

        httpServiceMock.getReadAccountBalance.mockReturnValue(of({
            amountPayments: Big('12.3'),
            amountPurchases: Big('45.6'),
            amountTotal: Big('-78.9')
        } as GetAccountBalanceResponse));

        fixture.detectChanges();

        expect(component).toBeTruthy();

        expect(component.purchases).toEqual([
            {purchaseId: 2, purchaseTimestamp: 456, productName: "product-2", productPrice: Big('543.21')},
            {purchaseId: 1, purchaseTimestamp: 123, productName: "product-1", productPrice: Big('123.45')}
        ] as GetPurchasesResponse[]);

        expect(component.accountBalance).toEqual({
            amountPayments: Big('12.3'),
            amountPurchases: Big('45.6'),
            amountTotal: Big('-78.9')
        } as GetAccountBalanceResponse);

        expect(component.isNegative).toBeTruthy();
    });

    it('should navigate to ' + routeName.purchases_delete, () => {

        routerNavigateSpy.mockReturnValue(firstValueFrom(of(true)));

        component.purchases = [
            {purchaseId: 1, purchaseTimestamp: 123, productName: "product-1%&/+=", productPrice: Big('123.45')},
            {purchaseId: 2, purchaseTimestamp: 456, productName: "product-2%&/+=", productPrice: Big('543.21')}
        ] as GetPurchasesResponse[];

        const urlAppend = encodeURIComponent(window.btoa(JSON.stringify({
            purchaseId: 1,
            purchaseTimestamp: 123,
            productName: "product-1%&/+=",
            productPrice: Big('123.45')
        })));

        component.onClick(0);

        expect(routerNavigateSpy).toHaveBeenCalledExactlyOnceWith(['/' + routeName.purchases_delete + '/' + urlAppend]);
    });
});

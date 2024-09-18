import {ComponentFixture, TestBed} from '@angular/core/testing';
import {provideRouter, Router} from "@angular/router";
import {Big} from "big.js";
import {MockProvider} from "ng-mocks";
import {firstValueFrom, of} from "rxjs";
import {routeName} from "../../app.routes";
import {HttpService} from "../../services/http.service";
import {GetAccountBalanceResponse} from "../../services/models/GetAccountBalanceResponse";
import {GetPurchasesResponse} from "../../services/models/GetPurchasesResponse";

import {PurchasesComponent} from './purchases.component';
import Spy = jasmine.Spy;
import SpyObj = jasmine.SpyObj;

describe('PurchasesComponent', () => {

    let component: PurchasesComponent;
    let fixture: ComponentFixture<PurchasesComponent>;

    let httpServiceSpy: SpyObj<HttpService>;

    let routerNavigateSpy: Spy;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [PurchasesComponent],
            providers: [
                MockProvider(HttpService),
                provideRouter([])
            ]
        })
            .compileComponents();

        fixture = TestBed.createComponent(PurchasesComponent);
        component = fixture.componentInstance;

        httpServiceSpy = spyOnAllFunctions(TestBed.inject(HttpService));

        routerNavigateSpy = spyOn(TestBed.inject(Router), 'navigate');
    });

    it('should create (amount total positive)', () => {

        httpServiceSpy.getReadPurchases.and.callFake(() => of([
            {purchaseId: 1, purchaseTimestamp: 123, productName: "product-1", productPrice: Big('123.45')},
            {purchaseId: 2, purchaseTimestamp: 456, productName: "product-2", productPrice: Big('543.21')}
        ] as GetPurchasesResponse[]));

        httpServiceSpy.getReadAccountBalance.and.callFake(() => of({
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

        expect(component.isNegative).toBeFalse();
    });

    it('should create (amount total negative)', () => {

        httpServiceSpy.getReadPurchases.and.callFake(() => of([
            {purchaseId: 1, purchaseTimestamp: 123, productName: "product-1", productPrice: Big('123.45')},
            {purchaseId: 2, purchaseTimestamp: 456, productName: "product-2", productPrice: Big('543.21')}
        ] as GetPurchasesResponse[]));

        httpServiceSpy.getReadAccountBalance.and.callFake(() => of({
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

        expect(component.isNegative).toBeTrue();
    });

    it('should navigate to ' + routeName.purchases_delete, () => {

        routerNavigateSpy.and.callFake(() => firstValueFrom(of(true)));

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

        expect(routerNavigateSpy).toHaveBeenCalledOnceWith(['/' + routeName.purchases_delete + '/' + urlAppend]);
    });
});

import {ComponentFixture, TestBed} from '@angular/core/testing';
import {provideRouter, Router} from "@angular/router";
import {Big} from "big.js";
import {MockProvider} from "ng-mocks";
import {firstValueFrom, of} from "rxjs";
import {routeName} from "../../app.routes";
import {HttpService} from "../../services/http.service";
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

    it('should create', () => {

        httpServiceSpy.getReadPurchases.and.callFake(() => of([
            {purchaseId: 1, purchaseTimestamp: 123, productName: "product-1", productPrice: Big('123.45')},
            {purchaseId: 2, purchaseTimestamp: 456, productName: "product-2", productPrice: Big('543.21')}
        ] as GetPurchasesResponse[]));

        fixture.detectChanges();

        expect(component).toBeTruthy();

        expect(component.purchases).toEqual([
            {purchaseId: 1, purchaseTimestamp: 123, productName: "product-1", productPrice: Big('123.45')},
            {purchaseId: 2, purchaseTimestamp: 456, productName: "product-2", productPrice: Big('543.21')}
        ] as GetPurchasesResponse[]);
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

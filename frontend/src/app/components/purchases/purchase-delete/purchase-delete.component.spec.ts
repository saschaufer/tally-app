import {ComponentFixture, TestBed} from '@angular/core/testing';
import {ActivatedRoute, provideRouter, Router} from "@angular/router";
import {Big} from "big.js";
import {MockProvider} from "ng-mocks";
import {firstValueFrom, of, throwError} from "rxjs";
import {routeName} from "../../../app.routes";
import {HttpService} from "../../../services/http.service";

import {PurchaseDeleteComponent} from './purchase-delete.component';
import Spy = jasmine.Spy;
import SpyObj = jasmine.SpyObj;

describe('PurchaseDeleteComponent', () => {

    let component: PurchaseDeleteComponent;
    let fixture: ComponentFixture<PurchaseDeleteComponent>;

    let httpServiceSpy: SpyObj<HttpService>;

    let routerNavigateSpy: Spy;

    beforeEach(async () => {

        const base64 = window.btoa(JSON.stringify({
            purchaseId: 1,
            purchaseTimestamp: 132,
            productName: "product-name",
            productPrice: Big('123.45')
        }));

        await TestBed.configureTestingModule({
            imports: [PurchaseDeleteComponent],
            providers: [
                MockProvider(HttpService),
                provideRouter([]),
                {provide: ActivatedRoute, useValue: {params: of({purchase: base64})}}
            ]
        })
            .compileComponents();

        fixture = TestBed.createComponent(PurchaseDeleteComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();

        httpServiceSpy = spyOnAllFunctions(TestBed.inject(HttpService));

        routerNavigateSpy = spyOn(TestBed.inject(Router), 'navigate');
    });

    it('should create', () => {

        expect(component).toBeTruthy();

        expect(component.purchase?.purchaseId).toBe(1);
        expect(component.purchase?.productName).toBe('product-name');
        expect(component.purchase?.productPrice).toEqual(Big('123.45'));
        expect(component.purchase?.purchaseTimestamp).toEqual(132);
    });

    it('should delete the purchase and navigate to ' + routeName.purchases, () => {

        httpServiceSpy.postDeletePurchase.and.callFake(() => of(undefined));
        routerNavigateSpy.and.callFake(() => firstValueFrom(of(true)));

        component.onClickDelete();

        expect(httpServiceSpy.postDeletePurchase).toHaveBeenCalledOnceWith(1);

        expect(routerNavigateSpy).toHaveBeenCalledOnceWith(['/' + routeName.purchases]);
    })

    it('should not navigate to ' + routeName.purchases + ' (delete purchase failed)', () => {

        httpServiceSpy.postDeletePurchase.and.callFake(() =>
            throwError(() => 'Error on delete purchase')
        );

        component.onClickDelete();

        expect(httpServiceSpy.postDeletePurchase).toHaveBeenCalledOnceWith(1);

        expect(routerNavigateSpy).not.toHaveBeenCalled();
    })
});

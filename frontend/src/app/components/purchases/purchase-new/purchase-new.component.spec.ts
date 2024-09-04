import {ComponentFixture, TestBed} from '@angular/core/testing';
import {provideRouter, Router} from "@angular/router";
import {Big} from "big.js";
import {MockProvider} from "ng-mocks";
import {firstValueFrom, of, throwError} from "rxjs";
import {routeName} from "../../../app.routes";
import {HttpService} from "../../../services/http.service";
import {GetProductsResponse} from "../../../services/models/GetProductsResponse";

import {PurchaseNewComponent} from './purchase-new.component';
import Spy = jasmine.Spy;
import SpyObj = jasmine.SpyObj;

describe('PurchaseNewComponent', () => {

    let component: PurchaseNewComponent;
    let fixture: ComponentFixture<PurchaseNewComponent>;

    let httpServiceSpy: SpyObj<HttpService>;

    let routerNavigateSpy: Spy;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [PurchaseNewComponent],
            providers: [
                MockProvider(HttpService),
                provideRouter([])
            ]
        })
            .compileComponents();

        fixture = TestBed.createComponent(PurchaseNewComponent);
        component = fixture.componentInstance;

        httpServiceSpy = spyOnAllFunctions(TestBed.inject(HttpService));

        routerNavigateSpy = spyOn(TestBed.inject(Router), 'navigate');
    });

    it('should create', () => {

        httpServiceSpy.getReadProducts.and.callFake(() => of([
            {id: 1, name: "product-1", price: Big('123.45')},
            {id: 2, name: "product-2", price: Big('678.90')}
        ] as GetProductsResponse[]));

        fixture.detectChanges();

        expect(component).toBeTruthy();

        expect(component.products).toEqual([
            {id: 1, name: "product-1", price: Big('123.45')},
            {id: 2, name: "product-2", price: Big('678.90')}
        ] as GetProductsResponse[]);
    });

    it('should select the clicked product', () => {

        component.products = [
            {id: 1, name: "product-1", price: Big('123.45')},
            {id: 2, name: "product-2", price: Big('678.90')}
        ] as GetProductsResponse[];

        component.onClick(0);

        expect(component.selectedProduct).toEqual({
            id: 1,
            name: "product-1",
            price: Big('123.45')
        } as GetProductsResponse);
    });

    it('should create the purchase and navigate to ' + routeName.purchases, () => {

        const dialog = document.getElementById('#purchases.purchaseNew.successCreatingPurchase')! as HTMLDialogElement;

        httpServiceSpy.postCreatePurchase.and.callFake(() => of(undefined));
        routerNavigateSpy.and.callFake(() => firstValueFrom(of(true)));

        component.selectedProduct = {id: 1, name: "product-1", price: Big('123.45')} as GetProductsResponse;

        component.onSubmit();

        expect(httpServiceSpy.postCreatePurchase).toHaveBeenCalledOnceWith(1);

        expect(routerNavigateSpy).not.toHaveBeenCalled();

        dialog.dispatchEvent(new Event('click'));

        expect(routerNavigateSpy).toHaveBeenCalledOnceWith(['/' + routeName.purchases]);
    });

    it('should not create the purchase and not navigate to ' + routeName.purchases + ' (no product selected)', () => {

        const dialog = document.getElementById('#purchases.purchaseNew.successCreatingPurchase')! as HTMLDialogElement;

        httpServiceSpy.postCreatePurchase.and.callFake(() => of(undefined));
        routerNavigateSpy.and.callFake(() => firstValueFrom(of(true)));

        component.onSubmit();

        dialog.dispatchEvent(new Event('click'));

        expect(httpServiceSpy.postCreatePurchase).not.toHaveBeenCalled();

        expect(routerNavigateSpy).not.toHaveBeenCalled();
    });

    it('should not navigate to ' + routeName.purchases + ' (create purchase failed)', () => {

        const dialog = document.getElementById('#purchases.purchaseNew.successCreatingPurchase')! as HTMLDialogElement;

        httpServiceSpy.postCreatePurchase.and.callFake(() =>
            throwError(() => 'Error on create purchase')
        );

        component.selectedProduct = {id: 1, name: "product-1", price: Big('123.45')} as GetProductsResponse;

        component.onSubmit();

        dialog.dispatchEvent(new Event('click'));

        expect(httpServiceSpy.postCreatePurchase).toHaveBeenCalledOnceWith(1);

        expect(routerNavigateSpy).not.toHaveBeenCalled();
    });
});

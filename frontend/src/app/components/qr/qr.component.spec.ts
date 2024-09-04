import {ComponentFixture, TestBed} from '@angular/core/testing';
import {ActivatedRoute, provideRouter, Router} from "@angular/router";
import {Big} from "big.js";
import {MockProvider} from "ng-mocks";
import {firstValueFrom, of, throwError} from "rxjs";
import {routeName} from "../../app.routes";
import {HttpService} from "../../services/http.service";
import {GetProductsResponse} from "../../services/models/GetProductsResponse";

import {QrComponent} from './qr.component';
import Spy = jasmine.Spy;
import SpyObj = jasmine.SpyObj;

describe('QrComponent', () => {

    let component: QrComponent;
    let fixture: ComponentFixture<QrComponent>;

    let httpServiceSpy: SpyObj<HttpService>;

    let router: ActivatedRoute;
    let routerNavigateSpy: Spy;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [QrComponent],
            providers: [
                MockProvider(HttpService),
                provideRouter([]),
                {provide: ActivatedRoute, useValue: {params: of({productId: 1})}}
            ]
        })
            .compileComponents();

        fixture = TestBed.createComponent(QrComponent);
        component = fixture.componentInstance;

        httpServiceSpy = spyOnAllFunctions(TestBed.inject(HttpService));

        router = TestBed.inject(ActivatedRoute);
        routerNavigateSpy = spyOn(TestBed.inject(Router), 'navigate');
    });

    it('should create and read product', () => {

        httpServiceSpy.postReadProduct.and.callFake(() => of({
            id: 1,
            name: "product-name",
            price: Big('123.45'),
        } as GetProductsResponse));

        fixture.detectChanges();

        expect(component).toBeTruthy();

        expect(component.product?.id).toBe(1);
        expect(component.product?.name).toBe('product-name');
        expect(component.product?.price).toEqual(Big('123.45'));

        expect(httpServiceSpy.postReadProduct).toHaveBeenCalledOnceWith(1);
    });

    it('should create and read product (read product failed)', () => {

        httpServiceSpy.postReadProduct.and.callFake(() =>
            throwError(() => 'Error on reading product')
        );

        fixture.detectChanges();

        expect(component.product).toBeFalsy();

        expect(httpServiceSpy.postReadProduct).toHaveBeenCalledOnceWith(1);
    })

    it('should create and not read product (no productId)', () => {

        router.params = of({});

        fixture.detectChanges();

        expect(component.product).toBeFalsy();

        expect(httpServiceSpy.postReadProduct).not.toHaveBeenCalled();
    })

    it('should create and not read product (productId not a number)', () => {

        router.params = of({productId: 'abc'});

        fixture.detectChanges();

        expect(component.product).toBeFalsy();

        expect(httpServiceSpy.postReadProduct).not.toHaveBeenCalled();
    })

    it('should create the purchase and navigate to ' + routeName.purchases, () => {

        const dialog = document.getElementById('#qr.successCreatingPurchase')! as HTMLDialogElement;

        httpServiceSpy.postCreatePurchase.and.callFake(() => of(undefined));
        routerNavigateSpy.and.callFake(() => firstValueFrom(of(true)));

        component.product = {id: 1, name: "product-name", price: Big('123.45')} as GetProductsResponse;

        component.onClickPurchase();

        expect(httpServiceSpy.postCreatePurchase).toHaveBeenCalledOnceWith(1);

        expect(routerNavigateSpy).not.toHaveBeenCalledOnceWith(['/' + routeName.purchases]);

        dialog.dispatchEvent(new Event('click'));

        expect(routerNavigateSpy).toHaveBeenCalledOnceWith(['/' + routeName.purchases]);
    });

    it('should not create the purchase and not navigate to ' + routeName.purchases + ' (no product selected)', () => {

        const dialog = document.getElementById('#qr.successCreatingPurchase')! as HTMLDialogElement;

        httpServiceSpy.postCreatePurchase.and.callFake(() => of(undefined));
        routerNavigateSpy.and.callFake(() => firstValueFrom(of(true)));

        component.onClickPurchase();

        dialog.dispatchEvent(new Event('click'));

        expect(httpServiceSpy.postCreatePurchase).not.toHaveBeenCalled();

        expect(routerNavigateSpy).not.toHaveBeenCalled();
    });

    it('should not navigate to ' + routeName.purchases + ' (create purchase failed)', () => {

        const dialog = document.getElementById('#qr.successCreatingPurchase')! as HTMLDialogElement;

        httpServiceSpy.postCreatePurchase.and.callFake(() =>
            throwError(() => 'Error on create purchase')
        );

        component.product = {id: 1, name: "product-1", price: Big('123.45')} as GetProductsResponse;

        component.onClickPurchase();

        dialog.dispatchEvent(new Event('click'));

        expect(httpServiceSpy.postCreatePurchase).toHaveBeenCalledOnceWith(1);

        expect(routerNavigateSpy).not.toHaveBeenCalled();
    });
});

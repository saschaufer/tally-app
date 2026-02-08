import {ComponentFixture, TestBed} from '@angular/core/testing';
import {ActivatedRoute, NavigationExtras, provideRouter, Router} from "@angular/router";
import {Big} from "big.js";
import {firstValueFrom, of, throwError} from "rxjs";
import {beforeEach, describe, expect, it, Mock, vi} from 'vitest';
import {routeName} from "../../app.routes";
import {HttpService} from "../../services/http.service";
import {GetProductsResponse} from "../../services/models/GetProductsResponse";

import {QrComponent} from './qr.component';

describe('QrComponent', () => {

    let component: QrComponent;
    let fixture: ComponentFixture<QrComponent>;

    const httpServiceMock = vi.mockObject(HttpService.prototype);

    let router: ActivatedRoute;
    let routerNavigateSpy: Mock<(commands: readonly any[], extras?: NavigationExtras) => Promise<boolean>>;
    let dialogSuccessCreatingPurchaseSpyShowModal: Mock<() => void>;
    let dialogSuccessCreatingPurchaseSpyClose: Mock<(returnValue?: string) => void>;

    beforeEach(async () => {

        vi.resetAllMocks();

        await TestBed.configureTestingModule({
            imports: [QrComponent],
            providers: [
                provideRouter([]),
                {provide: HttpService, useValue: httpServiceMock},
                {provide: ActivatedRoute, useValue: {params: of({productId: 1})}}
            ]
        })
            .compileComponents();

        fixture = TestBed.createComponent(QrComponent);
        component = fixture.componentInstance;

        router = TestBed.inject(ActivatedRoute);
        routerNavigateSpy = vi.spyOn(TestBed.inject(Router), 'navigate');
        dialogSuccessCreatingPurchaseSpyShowModal = vi.spyOn(component.dialogSuccessCreatingPurchase.nativeElement, 'showModal');
        dialogSuccessCreatingPurchaseSpyClose = vi.spyOn(component.dialogSuccessCreatingPurchase.nativeElement, 'close');
    });

    it('should create and read product', () => {

        httpServiceMock.postReadProduct.mockReturnValue(of({
            id: 1,
            name: "product-name",
            price: Big('123.45'),
        } as GetProductsResponse));

        fixture.detectChanges();

        expect(component).toBeTruthy();

        expect(component.product?.id).toBe(1);
        expect(component.product?.name).toBe('product-name');
        expect(component.product?.price).toEqual(Big('123.45'));

        expect(httpServiceMock.postReadProduct).toHaveBeenCalledExactlyOnceWith(1);
    });

    it('should create and read product (read product failed)', () => {

        httpServiceMock.postReadProduct.mockReturnValue(
            throwError(() => 'Error on reading product')
        );

        fixture.detectChanges();

        expect(component.product).toBeFalsy();

        expect(httpServiceMock.postReadProduct).toHaveBeenCalledExactlyOnceWith(1);
    })

    it('should create and not read product (no productId)', () => {

        router.params = of({});

        fixture.detectChanges();

        expect(component.product).toBeFalsy();

        expect(httpServiceMock.postReadProduct).not.toHaveBeenCalled();
    })

    it('should create and not read product (productId not a number)', () => {

        router.params = of({productId: 'abc'});

        fixture.detectChanges();

        expect(component.product).toBeFalsy();

        expect(httpServiceMock.postReadProduct).not.toHaveBeenCalled();
    })

    it('should create the purchase and navigate to ' + routeName.purchases, () => {

        const dialog: HTMLDialogElement = component.dialogSuccessCreatingPurchase.nativeElement;

        httpServiceMock.postCreatePurchase.mockReturnValue(of(undefined));
        routerNavigateSpy.mockReturnValue(firstValueFrom(of(true)));

        component.product = {id: 1, name: "product-name", price: Big('123.45')} as GetProductsResponse;

        expect(dialogSuccessCreatingPurchaseSpyShowModal).not.toHaveBeenCalled();
        expect(dialogSuccessCreatingPurchaseSpyClose).not.toHaveBeenCalled();

        component.onClickPurchase();

        expect(httpServiceMock.postCreatePurchase).toHaveBeenCalledExactlyOnceWith(1);

        expect(routerNavigateSpy).not.toHaveBeenCalledExactlyOnceWith(['/' + routeName.purchases]);

        expect(dialogSuccessCreatingPurchaseSpyShowModal).toHaveBeenCalled();
        expect(dialogSuccessCreatingPurchaseSpyClose).not.toHaveBeenCalled();

        dialog.dispatchEvent(new Event('click'));

        expect(dialogSuccessCreatingPurchaseSpyClose).toHaveBeenCalled();

        expect(routerNavigateSpy).toHaveBeenCalledExactlyOnceWith(['/' + routeName.purchases]);
    });

    it('should not create the purchase and not navigate to ' + routeName.purchases + ' (no product selected)', () => {

        httpServiceMock.postCreatePurchase.mockReturnValue(of(undefined));
        routerNavigateSpy.mockReturnValue(firstValueFrom(of(true)));

        component.onClickPurchase();

        expect(httpServiceMock.postCreatePurchase).not.toHaveBeenCalled();

        expect(routerNavigateSpy).not.toHaveBeenCalled();

        expect(dialogSuccessCreatingPurchaseSpyShowModal).not.toHaveBeenCalled();
        expect(dialogSuccessCreatingPurchaseSpyClose).not.toHaveBeenCalled();
    });

    it('should not navigate to ' + routeName.purchases + ' (create purchase failed)', () => {

        httpServiceMock.postCreatePurchase.mockReturnValue(
            throwError(() => 'Error on create purchase')
        );

        component.product = {id: 1, name: "product-1", price: Big('123.45')} as GetProductsResponse;

        component.onClickPurchase();

        expect(httpServiceMock.postCreatePurchase).toHaveBeenCalledExactlyOnceWith(1);

        expect(routerNavigateSpy).not.toHaveBeenCalled();

        expect(dialogSuccessCreatingPurchaseSpyShowModal).not.toHaveBeenCalled();
        expect(dialogSuccessCreatingPurchaseSpyClose).not.toHaveBeenCalled();
    });
});

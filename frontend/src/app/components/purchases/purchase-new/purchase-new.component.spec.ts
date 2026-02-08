import {ComponentFixture, TestBed} from '@angular/core/testing';
import {NavigationExtras, provideRouter, Router} from "@angular/router";
import {Mock} from "@vitest/spy";
import {Big} from "big.js";
import {firstValueFrom, of, throwError} from "rxjs";
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {routeName} from "../../../app.routes";
import {HttpService} from "../../../services/http.service";
import {GetProductsResponse} from "../../../services/models/GetProductsResponse";

import {PurchaseNewComponent} from './purchase-new.component';

describe('PurchaseNewComponent', () => {

    let component: PurchaseNewComponent;
    let fixture: ComponentFixture<PurchaseNewComponent>;

    const httpServiceMock = vi.mockObject(HttpService.prototype);

    let routerNavigateSpy: Mock<(commands: readonly any[], extras?: NavigationExtras) => Promise<boolean>>;
    let dialogErrorReadingProductsSpyShowModal: Mock<() => void>;
    let dialogErrorReadingProductsSpyClose: Mock<(returnValue?: string) => void>;
    let dialogErrorNoProductSelectedSpyShowModal: Mock<() => void>;
    let dialogErrorNoProductSelectedSpyClose: Mock<(returnValue?: string) => void>;
    let dialogSuccessCreatingPurchaseSpyShowModal: Mock<() => void>;
    let dialogSuccessCreatingPurchaseSpyClose: Mock<(returnValue?: string) => void>;
    let dialogErrorCreatingPurchaseSpyShowModal: Mock<() => void>;
    let dialogErrorCreatingPurchaseSpyClose: Mock<(returnValue?: string) => void>;

    beforeEach(async () => {

        vi.resetAllMocks();

        await TestBed.configureTestingModule({
            imports: [PurchaseNewComponent],
            providers: [
                provideRouter([]),
                {provide: HttpService, useValue: httpServiceMock}
            ]
        })
            .compileComponents();

        fixture = TestBed.createComponent(PurchaseNewComponent);
        component = fixture.componentInstance;

        routerNavigateSpy = vi.spyOn(TestBed.inject(Router), 'navigate');
        dialogErrorReadingProductsSpyShowModal = vi.spyOn(component.dialogErrorReadingProducts.nativeElement, 'showModal');
        dialogErrorReadingProductsSpyClose = vi.spyOn(component.dialogErrorReadingProducts.nativeElement, 'close');
        dialogErrorNoProductSelectedSpyShowModal = vi.spyOn(component.dialogErrorNoProductSelected.nativeElement, 'showModal');
        dialogErrorNoProductSelectedSpyClose = vi.spyOn(component.dialogErrorNoProductSelected.nativeElement, 'close');
        dialogSuccessCreatingPurchaseSpyShowModal = vi.spyOn(component.dialogSuccessCreatingPurchase.nativeElement, 'showModal');
        dialogSuccessCreatingPurchaseSpyClose = vi.spyOn(component.dialogSuccessCreatingPurchase.nativeElement, 'close');
        dialogErrorCreatingPurchaseSpyShowModal = vi.spyOn(component.dialogErrorCreatingPurchase.nativeElement, 'showModal');
        dialogErrorCreatingPurchaseSpyClose = vi.spyOn(component.dialogErrorCreatingPurchase.nativeElement, 'close');
    });

    it('should create', () => {

        httpServiceMock.getReadProducts.mockReturnValue(of([
            {id: 1, name: "bb-product-1", price: Big('123.45')},
            {id: 2, name: "aa-product-2", price: Big('678.90')}
        ] as GetProductsResponse[]));

        fixture.detectChanges();

        expect(component).toBeTruthy();

        expect(component.products).toEqual([
            {id: 2, name: "aa-product-2", price: Big('678.90')},
            {id: 1, name: "bb-product-1", price: Big('123.45')}
        ] as GetProductsResponse[]);

        expect(dialogErrorReadingProductsSpyShowModal).not.toHaveBeenCalled();
        expect(dialogErrorReadingProductsSpyClose).not.toHaveBeenCalled();
        expect(dialogErrorNoProductSelectedSpyShowModal).not.toHaveBeenCalled();
        expect(dialogErrorNoProductSelectedSpyClose).not.toHaveBeenCalled();
        expect(dialogSuccessCreatingPurchaseSpyShowModal).not.toHaveBeenCalled();
        expect(dialogSuccessCreatingPurchaseSpyClose).not.toHaveBeenCalled();
        expect(dialogErrorCreatingPurchaseSpyShowModal).not.toHaveBeenCalled();
        expect(dialogErrorCreatingPurchaseSpyClose).not.toHaveBeenCalled();
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

        const dialog: HTMLDialogElement = component.dialogSuccessCreatingPurchase.nativeElement;

        httpServiceMock.getReadProducts.mockReturnValue(of([
            {id: 1, name: "bb-product-1", price: Big('123.45')},
            {id: 2, name: "aa-product-2", price: Big('678.90')}
        ] as GetProductsResponse[]));

        httpServiceMock.postCreatePurchase.mockReturnValue(of(undefined));
        routerNavigateSpy.mockReturnValue(firstValueFrom(of(true)));

        component.selectedProduct = {id: 1, name: "product-1", price: Big('123.45')} as GetProductsResponse;

        fixture.detectChanges();

        expect(dialogSuccessCreatingPurchaseSpyShowModal).not.toHaveBeenCalled();
        expect(dialogSuccessCreatingPurchaseSpyClose).not.toHaveBeenCalled();

        component.onSubmit();

        expect(httpServiceMock.postCreatePurchase).toHaveBeenCalledExactlyOnceWith(1);

        expect(routerNavigateSpy).not.toHaveBeenCalled();

        expect(dialogSuccessCreatingPurchaseSpyShowModal).toHaveBeenCalled();
        expect(dialogSuccessCreatingPurchaseSpyClose).not.toHaveBeenCalled();

        dialog.dispatchEvent(new Event('click'));

        expect(routerNavigateSpy).toHaveBeenCalledExactlyOnceWith(['/' + routeName.purchases]);

        expect(dialogErrorReadingProductsSpyShowModal).not.toHaveBeenCalled();
        expect(dialogErrorReadingProductsSpyClose).not.toHaveBeenCalled();
        expect(dialogErrorNoProductSelectedSpyShowModal).not.toHaveBeenCalled();
        expect(dialogErrorNoProductSelectedSpyClose).not.toHaveBeenCalled();
        expect(dialogSuccessCreatingPurchaseSpyShowModal).toHaveBeenCalled();
        expect(dialogSuccessCreatingPurchaseSpyClose).toHaveBeenCalled();
        expect(dialogErrorCreatingPurchaseSpyShowModal).not.toHaveBeenCalled();
        expect(dialogErrorCreatingPurchaseSpyClose).not.toHaveBeenCalled();
    });

    it('should not create the purchase and not navigate to ' + routeName.purchases + ' (no product selected)', () => {

        const dialog: HTMLDialogElement = component.dialogSuccessCreatingPurchase.nativeElement;

        httpServiceMock.postCreatePurchase.mockReturnValue(of(undefined));
        routerNavigateSpy.mockReturnValue(firstValueFrom(of(true)));

        component.onSubmit();

        dialog.dispatchEvent(new Event('click'));

        expect(httpServiceMock.postCreatePurchase).not.toHaveBeenCalled();

        expect(routerNavigateSpy).not.toHaveBeenCalled();
    });

    it('should not navigate to ' + routeName.purchases + ' (create purchase failed)', () => {

        const dialog: HTMLDialogElement = component.dialogSuccessCreatingPurchase.nativeElement;

        httpServiceMock.postCreatePurchase.mockReturnValue(
            throwError(() => 'Error on create purchase')
        );

        component.selectedProduct = {id: 1, name: "product-1", price: Big('123.45')} as GetProductsResponse;

        component.onSubmit();

        dialog.dispatchEvent(new Event('click'));

        expect(httpServiceMock.postCreatePurchase).toHaveBeenCalledExactlyOnceWith(1);

        expect(routerNavigateSpy).not.toHaveBeenCalled();
    });
});

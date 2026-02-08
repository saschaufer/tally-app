import {ComponentFixture, TestBed} from '@angular/core/testing';
import {ActivatedRoute, NavigationExtras, provideRouter, Router} from "@angular/router";
import {Big} from "big.js";
import {firstValueFrom, of, throwError} from "rxjs";
import {beforeEach, describe, expect, it, Mock, vi} from 'vitest';
import {routeName} from "../../../app.routes";
import {HttpService} from "../../../services/http.service";

import {PurchaseDeleteComponent} from './purchase-delete.component';

describe('PurchaseDeleteComponent', () => {

    let component: PurchaseDeleteComponent;
    let fixture: ComponentFixture<PurchaseDeleteComponent>;

    const httpServiceMock = vi.mockObject(HttpService.prototype);

    let routerNavigateSpy: Mock<(commands: readonly any[], extras?: NavigationExtras) => Promise<boolean>>;
    let dialogSuccessSpyShowModal: Mock<() => void>;
    let dialogSuccessSpyClose: Mock<(returnValue?: string) => void>;
    let dialogErrorSpyShowModal: Mock<() => void>;
    let dialogErrorSpyClose: Mock<(returnValue?: string) => void>;

    beforeEach(async () => {

        vi.resetAllMocks();

        const urlAppend = encodeURIComponent(window.btoa(JSON.stringify({
            purchaseId: 1,
            purchaseTimestamp: 132,
            productName: "product-name%&/+=",
            productPrice: Big('123.45')
        })));

        await TestBed.configureTestingModule({
            imports: [PurchaseDeleteComponent],
            providers: [
                provideRouter([]),
                {provide: HttpService, useValue: httpServiceMock},
                {provide: ActivatedRoute, useValue: {params: of({purchase: urlAppend})}}
            ]
        })
            .compileComponents();

        fixture = TestBed.createComponent(PurchaseDeleteComponent);
        component = fixture.componentInstance;

        routerNavigateSpy = vi.spyOn(TestBed.inject(Router), 'navigate');
        dialogSuccessSpyShowModal = vi.spyOn(component.dialogSuccess.nativeElement, 'showModal');
        dialogSuccessSpyClose = vi.spyOn(component.dialogSuccess.nativeElement, 'close');
        dialogErrorSpyShowModal = vi.spyOn(component.dialogError.nativeElement, 'showModal');
        dialogErrorSpyClose = vi.spyOn(component.dialogError.nativeElement, 'close');

        fixture.detectChanges();
    });

    it('should create', () => {

        expect(component).toBeTruthy();

        expect(component.purchase?.purchaseId).toBe(1);
        expect(component.purchase?.productName).toBe('product-name%&/+=');
        expect(component.purchase?.productPrice).toEqual(Big('123.45'));
        expect(component.purchase?.purchaseTimestamp).toEqual(132);
    });

    it('should delete the purchase and navigate to ' + routeName.purchases, () => {

        const dialog: HTMLDialogElement = component.dialogSuccess.nativeElement;

        httpServiceMock.postDeletePurchase.mockReturnValue(of(undefined));
        routerNavigateSpy.mockReturnValue(firstValueFrom(of(true)));

        expect(dialogSuccessSpyShowModal).not.toHaveBeenCalled();
        expect(dialogSuccessSpyClose).not.toHaveBeenCalled();
        expect(dialogErrorSpyShowModal).not.toHaveBeenCalled();
        expect(dialogErrorSpyClose).not.toHaveBeenCalled();

        component.onClickDelete();

        expect(httpServiceMock.postDeletePurchase).toHaveBeenCalledExactlyOnceWith(1);

        expect(routerNavigateSpy).not.toHaveBeenCalled();

        expect(dialogSuccessSpyShowModal).toHaveBeenCalled();
        expect(dialogSuccessSpyClose).not.toHaveBeenCalled();
        expect(dialogErrorSpyShowModal).not.toHaveBeenCalled();
        expect(dialogErrorSpyClose).not.toHaveBeenCalled();

        dialog.dispatchEvent(new Event('click'));

        expect(dialogSuccessSpyShowModal).toHaveBeenCalled();
        expect(dialogSuccessSpyClose).toHaveBeenCalled();
        expect(dialogErrorSpyShowModal).not.toHaveBeenCalled();
        expect(dialogErrorSpyClose).not.toHaveBeenCalled();

        expect(routerNavigateSpy).toHaveBeenCalledExactlyOnceWith(['/' + routeName.purchases]);
    })

    it('should not navigate to ' + routeName.purchases + ' (delete purchase failed)', () => {

        const dialog: HTMLDialogElement = component.dialogError.nativeElement;

        httpServiceMock.postDeletePurchase.mockReturnValue(
            throwError(() => 'Error on delete purchase')
        );

        expect(dialogSuccessSpyShowModal).not.toHaveBeenCalled();
        expect(dialogSuccessSpyClose).not.toHaveBeenCalled();
        expect(dialogErrorSpyShowModal).not.toHaveBeenCalled();
        expect(dialogErrorSpyClose).not.toHaveBeenCalled();

        component.onClickDelete();

        expect(dialogSuccessSpyShowModal).not.toHaveBeenCalled();
        expect(dialogSuccessSpyClose).not.toHaveBeenCalled();
        expect(dialogErrorSpyShowModal).toHaveBeenCalled();
        expect(dialogErrorSpyClose).not.toHaveBeenCalled();

        expect(httpServiceMock.postDeletePurchase).toHaveBeenCalledExactlyOnceWith(1);

        dialog.dispatchEvent(new Event('click'));

        expect(dialogSuccessSpyShowModal).not.toHaveBeenCalled();
        expect(dialogSuccessSpyClose).not.toHaveBeenCalled();
        expect(dialogErrorSpyShowModal).toHaveBeenCalled();
        expect(dialogErrorSpyClose).toHaveBeenCalled();

        expect(routerNavigateSpy).not.toHaveBeenCalled();
    })
});

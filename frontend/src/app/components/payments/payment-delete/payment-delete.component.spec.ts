import {ComponentFixture, TestBed} from '@angular/core/testing';
import {ActivatedRoute, provideRouter, Router} from "@angular/router";
import {Big} from "big.js";
import {MockProvider} from "ng-mocks";
import {firstValueFrom, of, throwError} from "rxjs";
import {routeName} from "../../../app.routes";
import {HttpService} from "../../../services/http.service";

import {PaymentDeleteComponent} from './payment-delete.component';
import Spy = jasmine.Spy;
import SpyObj = jasmine.SpyObj;

describe('PaymentDeleteComponent', () => {

    let component: PaymentDeleteComponent;
    let fixture: ComponentFixture<PaymentDeleteComponent>;

    let httpServiceSpy: SpyObj<HttpService>;

    let routerNavigateSpy: Spy;

    beforeEach(async () => {

        const urlAppend = encodeURIComponent(window.btoa(JSON.stringify({
            id: 1,
            amount: Big('123.45'),
            timestamp: 132
        })));

        await TestBed.configureTestingModule({
            imports: [PaymentDeleteComponent],
            providers: [
                MockProvider(HttpService),
                provideRouter([]),
                {provide: ActivatedRoute, useValue: {params: of({payment: urlAppend})}}
            ]
        })
            .compileComponents();

        fixture = TestBed.createComponent(PaymentDeleteComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();

        httpServiceSpy = spyOnAllFunctions(TestBed.inject(HttpService));

        routerNavigateSpy = spyOn(TestBed.inject(Router), 'navigate');
    });

    it('should create', () => {

        expect(component).toBeTruthy();

        expect(component.payment?.id).toBe(1);
        expect(component.payment?.amount).toEqual(Big('123.45'));
        expect(component.payment?.timestamp).toEqual(132);
    });

    it('should delete the payment and navigate to ' + routeName.payments, () => {

        const dialog = document.getElementById('#payments.paymentDelete.successDeletePayment')! as HTMLDialogElement;

        httpServiceSpy.postDeletePayment.and.callFake(() => of(undefined));
        routerNavigateSpy.and.callFake(() => firstValueFrom(of(true)));

        component.onClickDelete();

        expect(httpServiceSpy.postDeletePayment).toHaveBeenCalledOnceWith(1);

        expect(routerNavigateSpy).not.toHaveBeenCalled();

        dialog.dispatchEvent(new Event('click'));

        expect(routerNavigateSpy).toHaveBeenCalledOnceWith(['/' + routeName.payments]);
    })

    it('should not navigate to ' + routeName.payments + ' (delete payment failed)', () => {

        const dialog = document.getElementById('#payments.paymentDelete.successDeletePayment')! as HTMLDialogElement;

        httpServiceSpy.postDeletePayment.and.callFake(() =>
            throwError(() => 'Error on delete payment')
        );

        component.onClickDelete();

        expect(httpServiceSpy.postDeletePayment).toHaveBeenCalledOnceWith(1);

        dialog.dispatchEvent(new Event('click'));

        expect(routerNavigateSpy).not.toHaveBeenCalled();
    })
});

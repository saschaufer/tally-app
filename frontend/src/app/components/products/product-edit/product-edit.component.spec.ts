import {ComponentFixture, TestBed} from '@angular/core/testing';
import {ActivatedRoute, provideRouter} from "@angular/router";
import {Big} from "big.js";
import {MockProvider} from "ng-mocks";
import {of, throwError} from "rxjs";
import {HttpService} from "../../../services/http.service";

import {ProductEditComponent} from './product-edit.component';
import SpyObj = jasmine.SpyObj;

describe('ProductEditComponent', () => {

    let component: ProductEditComponent;
    let fixture: ComponentFixture<ProductEditComponent>;

    let httpServiceSpy: SpyObj<HttpService>;

    beforeEach(async () => {

        const base64 = window.btoa(JSON.stringify({id: 1, name: "product-name", price: Big('123.45')}));

        await TestBed.configureTestingModule({
            imports: [ProductEditComponent],
            providers: [
                MockProvider(HttpService),
                provideRouter([]),
                {provide: ActivatedRoute, useValue: {params: of({product: base64})}}
            ]
        })
            .compileComponents();

        fixture = TestBed.createComponent(ProductEditComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();

        httpServiceSpy = spyOnAllFunctions(TestBed.inject(HttpService));
    });

    it('should create', () => {

        expect(component).toBeTruthy();

        expect(component.product?.id).toBe(1);
        expect(component.product?.name).toBe('product-name');
        expect(component.product?.price).toEqual(Big('123.45'));

        expect(component.changeNameForm.controls.name.value).toBe('product-name');
        expect(component.changePriceForm.controls.price.value).toBe('123.45');
    });

    it('should change the product name', () => {

        httpServiceSpy.postUpdateProduct.and.callFake(() => of());

        component.product = {id: 1, name: "product-name", price: Big('123.45')};
        component.changeNameForm.controls.name.patchValue('new-product-name');

        component.onSubmitChangeName();

        expect(httpServiceSpy.postUpdateProduct).toHaveBeenCalledOnceWith(1, 'new-product-name');
    });

    it('should not change the product name (name wrong)', () => {

        httpServiceSpy.postUpdateProduct.and.callFake(() => of());

        component.changeNameForm.controls.name.setErrors(['wrong']);

        component.onSubmitChangeName();

        expect(httpServiceSpy.postUpdateProduct).not.toHaveBeenCalled();
    });

    it('should not change the product name (change name failed)', () => {

        httpServiceSpy.postUpdateProduct.and.callFake(() =>
            throwError(() => 'Error on change name')
        );

        component.product = {id: 1, name: "product-name", price: Big('123.45')};
        component.changeNameForm.controls.name.patchValue('new-product-name');

        component.onSubmitChangeName();

        expect(httpServiceSpy.postUpdateProduct).toHaveBeenCalledOnceWith(1, 'new-product-name');
    });

    it('should change the product price', () => {

        httpServiceSpy.postUpdateProductPrice.and.callFake(() => of());

        component.product = {id: 1, name: "product-name", price: Big('123.45')};
        component.changePriceForm.controls.price.patchValue('111');

        component.onSubmitChangePrice();

        expect(httpServiceSpy.postUpdateProductPrice).toHaveBeenCalledOnceWith(1, Big('111'));
    });

    it('should not change the product price (price wrong)', () => {

        httpServiceSpy.postUpdateProductPrice.and.callFake(() => of());

        component.changePriceForm.controls.price.setErrors(['wrong']);

        component.onSubmitChangePrice();

        expect(httpServiceSpy.postUpdateProductPrice).not.toHaveBeenCalled();
    });

    it('should not change the product price (change price failed)', () => {

        httpServiceSpy.postUpdateProductPrice.and.callFake(() =>
            throwError(() => 'Error on change price')
        );

        component.product = {id: 1, name: "product-name", price: Big('123.45')};
        component.changePriceForm.controls.price.patchValue('111');

        component.onSubmitChangePrice();

        expect(httpServiceSpy.postUpdateProductPrice).toHaveBeenCalledOnceWith(1, Big('111'));
    });
});

import {ComponentFixture, TestBed} from '@angular/core/testing';
import {ActivatedRoute, provideRouter} from "@angular/router";
import {Big} from "big.js";
import {of, throwError} from "rxjs";
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {HttpService} from "../../../services/http.service";

import {ProductEditComponent} from './product-edit.component';

describe('ProductEditComponent', () => {

    let component: ProductEditComponent;
    let fixture: ComponentFixture<ProductEditComponent>;

    const httpServiceMock = vi.mockObject(HttpService.prototype);

    beforeEach(async () => {

        vi.resetAllMocks();

        const urlAppend = encodeURIComponent(window.btoa(JSON.stringify({
            id: 1,
            name: "product-name%&/+=",
            price: Big('123.45')
        })));

        await TestBed.configureTestingModule({
            imports: [ProductEditComponent],
            providers: [
                provideRouter([]),
                {provide: HttpService, useValue: httpServiceMock},
                {provide: ActivatedRoute, useValue: {params: of({product: urlAppend})}}
            ]
        })
            .compileComponents();

        fixture = TestBed.createComponent(ProductEditComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {

        expect(component).toBeTruthy();

        expect(component.product?.id).toBe(1);
        expect(component.product?.name).toBe('product-name%&/+=');
        expect(component.product?.price).toEqual(Big('123.45'));

        expect(component.changeNameForm.controls.name.value).toBe('product-name%&/+=');
        expect(component.changePriceForm.controls.price.value).toBe('123.45');
    });

    it('should change the product name', () => {

        httpServiceMock.postUpdateProduct.mockReturnValue(of(undefined));

        component.product = {id: 1, name: "product-name", price: Big('123.45')};
        component.changeNameForm.controls.name.patchValue('new-product-name');

        component.onSubmitChangeName();

        expect(httpServiceMock.postUpdateProduct).toHaveBeenCalledExactlyOnceWith(1, 'new-product-name');
    });

    it('should not change the product name (name wrong)', () => {

        httpServiceMock.postUpdateProduct.mockReturnValue(of(undefined));

        component.changeNameForm.controls.name.setErrors(['wrong']);

        component.onSubmitChangeName();

        expect(httpServiceMock.postUpdateProduct).not.toHaveBeenCalled();
    });

    it('should not change the product name (change name failed)', () => {

        httpServiceMock.postUpdateProduct.mockReturnValue(
            throwError(() => 'Error on change name')
        );

        component.product = {id: 1, name: "product-name", price: Big('123.45')};
        component.changeNameForm.controls.name.patchValue('new-product-name');

        component.onSubmitChangeName();

        expect(httpServiceMock.postUpdateProduct).toHaveBeenCalledExactlyOnceWith(1, 'new-product-name');
    });

    it('should change the product price', () => {

        httpServiceMock.postUpdateProductPrice.mockReturnValue(of(undefined));

        component.product = {id: 1, name: "product-name", price: Big('123.45')};
        component.changePriceForm.controls.price.patchValue('111');

        component.onSubmitChangePrice();

        expect(httpServiceMock.postUpdateProductPrice).toHaveBeenCalledExactlyOnceWith(1, Big('111'));
    });

    it('should not change the product price (price wrong)', () => {

        httpServiceMock.postUpdateProductPrice.mockReturnValue(of(undefined));

        component.changePriceForm.controls.price.setErrors(['wrong']);

        component.onSubmitChangePrice();

        expect(httpServiceMock.postUpdateProductPrice).not.toHaveBeenCalled();
    });

    it('should not change the product price (change price failed)', () => {

        httpServiceMock.postUpdateProductPrice.mockReturnValue(
            throwError(() => 'Error on change price')
        );

        component.product = {id: 1, name: "product-name", price: Big('123.45')};
        component.changePriceForm.controls.price.patchValue('111');

        component.onSubmitChangePrice();

        expect(httpServiceMock.postUpdateProductPrice).toHaveBeenCalledExactlyOnceWith(1, Big('111'));
    });

    it('should delete the product', () => {

        httpServiceMock.postDeleteProduct.mockReturnValue(of(undefined));

        component.product = {id: 1, name: "product-name", price: Big('123.45')};

        component.onClickDelete();

        expect(httpServiceMock.postDeleteProduct).toHaveBeenCalledExactlyOnceWith(1);
    });

    it('should not delete the product (delete product failed)', () => {

        httpServiceMock.postDeleteProduct.mockReturnValue(
            throwError(() => 'Error on delete product')
        );

        component.product = {id: 1, name: "product-name", price: Big('123.45')};

        component.onClickDelete();

        expect(httpServiceMock.postDeleteProduct).toHaveBeenCalledExactlyOnceWith(1);
    });
});

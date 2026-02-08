import {ComponentFixture, TestBed} from '@angular/core/testing';
import {provideRouter} from "@angular/router";
import {Big} from "big.js";
import {of, throwError} from "rxjs";
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {HttpService} from "../../../services/http.service";

import {ProductNewComponent} from './product-new.component';

describe('ProductNewComponent', () => {

    let component: ProductNewComponent;
    let fixture: ComponentFixture<ProductNewComponent>;

    const httpServiceMock = vi.mockObject(HttpService.prototype);

    beforeEach(async () => {

        vi.resetAllMocks();

        await TestBed.configureTestingModule({
            imports: [ProductNewComponent],
            providers: [
                provideRouter([]),
                {provide: HttpService, useValue: httpServiceMock}
            ]
        })
            .compileComponents();

        fixture = TestBed.createComponent(ProductNewComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    it('should create the product', () => {

        httpServiceMock.postCreateProduct.mockReturnValue(of(undefined));

        component.newProductForm.setValue({
            name: 'new-product',
            price: '123.45'
        });

        component.onSubmit();

        expect(httpServiceMock.postCreateProduct).toHaveBeenCalledExactlyOnceWith('new-product', Big('123.45'));
    });

    it('should not create the product (name wrong)', () => {

        httpServiceMock.postCreateProduct.mockReturnValue(of(undefined));

        component.newProductForm.controls.name.setErrors(['wrong']);
        component.newProductForm.controls.price.patchValue('123.45');

        component.onSubmit();

        expect(httpServiceMock.postCreateProduct).not.toHaveBeenCalled();
    });

    it('should not create the product (price wrong)', () => {

        httpServiceMock.postCreateProduct.mockReturnValue(of(undefined));

        component.newProductForm.controls.name.patchValue('new-product');
        component.newProductForm.controls.price.setErrors(['wrong']);

        component.onSubmit();

        expect(httpServiceMock.postCreateProduct).not.toHaveBeenCalled();
    });

    it('should not create the product (create product failed)', () => {

        httpServiceMock.postCreateProduct.mockReturnValue(
            throwError(() => 'Error on create product')
        );

        component.newProductForm.controls.name.patchValue('new-product');
        component.newProductForm.controls.price.patchValue('123.45');

        component.onSubmit();

        expect(httpServiceMock.postCreateProduct).toHaveBeenCalledExactlyOnceWith('new-product', Big('123.45'));
    });
});

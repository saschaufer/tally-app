import {ComponentFixture, TestBed} from '@angular/core/testing';
import {provideRouter} from "@angular/router";
import {Big} from "big.js";
import {MockProvider} from "ng-mocks";
import {of, throwError} from "rxjs";
import {HttpService} from "../../../services/http.service";

import {ProductNewComponent} from './product-new.component';
import SpyObj = jasmine.SpyObj;

describe('ProductNewComponent', () => {

    let component: ProductNewComponent;
    let fixture: ComponentFixture<ProductNewComponent>;

    let httpServiceSpy: SpyObj<HttpService>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [ProductNewComponent],
            providers: [
                MockProvider(HttpService),
                provideRouter([])
            ]
        })
            .compileComponents();

        fixture = TestBed.createComponent(ProductNewComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();

        httpServiceSpy = spyOnAllFunctions(TestBed.inject(HttpService));
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    it('should create the product', () => {

        httpServiceSpy.postCreateProduct.and.callFake(() => of());

        component.newProductForm.setValue({
            name: 'new-product',
            price: '123.45'
        });

        component.onSubmit();

        expect(httpServiceSpy.postCreateProduct).toHaveBeenCalledOnceWith('new-product', Big('123.45'));
    });

    it('should not create the product (name wrong)', () => {

        httpServiceSpy.postCreateProduct.and.callFake(() => of());

        component.newProductForm.controls.name.setErrors(['wrong']);
        component.newProductForm.controls.price.patchValue('123.45');

        component.onSubmit();

        expect(httpServiceSpy.postCreateProduct).not.toHaveBeenCalled();
    });

    it('should not create the product (price wrong)', () => {

        httpServiceSpy.postCreateProduct.and.callFake(() => of());

        component.newProductForm.controls.name.patchValue('new-product');
        component.newProductForm.controls.price.setErrors(['wrong']);

        component.onSubmit();

        expect(httpServiceSpy.postCreateProduct).not.toHaveBeenCalled();
    });

    it('should not create the product (create product failed)', () => {

        httpServiceSpy.postCreateProduct.and.callFake(() =>
            throwError(() => 'Error on create product')
        );

        component.newProductForm.controls.name.patchValue('new-product');
        component.newProductForm.controls.price.patchValue('123.45');

        component.onSubmit();

        expect(httpServiceSpy.postCreateProduct).toHaveBeenCalledOnceWith('new-product', Big('123.45'));
    });
});

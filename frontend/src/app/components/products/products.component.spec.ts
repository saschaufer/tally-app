import {ComponentFixture, TestBed} from '@angular/core/testing';
import {provideRouter, Router} from "@angular/router";
import {Big} from "big.js";
import {MockProvider} from "ng-mocks";
import {firstValueFrom, of} from "rxjs";
import {routeName} from "../../app.routes";
import {HttpService} from "../../services/http.service";
import {GetProductsResponse} from "../../services/models/GetProductsResponse";

import {ProductsComponent} from './products.component';
import Spy = jasmine.Spy;
import SpyObj = jasmine.SpyObj;

describe('ProductsComponent', () => {

    let component: ProductsComponent;
    let fixture: ComponentFixture<ProductsComponent>;

    let httpServiceSpy: SpyObj<HttpService>;

    let routerNavigateSpy: Spy;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [ProductsComponent],
            providers: [
                MockProvider(HttpService),
                provideRouter([])
            ]
        })
            .compileComponents();

        fixture = TestBed.createComponent(ProductsComponent);
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

    it('should navigate to ' + routeName.products_edit, () => {

        routerNavigateSpy.and.callFake(() => firstValueFrom(of(true)));

        component.products = [
            {id: 1, name: "product-1", price: Big('123.45')},
            {id: 2, name: "product-2", price: Big('678.90')}
        ] as GetProductsResponse[];

        const base64 = window.btoa(JSON.stringify({id: 1, name: "product-1", price: Big('123.45')}));

        component.onClick(0);

        expect(routerNavigateSpy).toHaveBeenCalledOnceWith(['/' + routeName.products_edit + '/' + base64]);
    });
});

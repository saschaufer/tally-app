import {ComponentFixture, TestBed} from '@angular/core/testing';
import {NavigationExtras, provideRouter, Router} from "@angular/router";
import {Mock} from "@vitest/spy";
import {Big} from "big.js";
import {firstValueFrom, of} from "rxjs";
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {routeName} from "../../app.routes";
import {HttpService} from "../../services/http.service";
import {GetProductsResponse} from "../../services/models/GetProductsResponse";

import {ProductsComponent} from './products.component';

describe('ProductsComponent', () => {

    let component: ProductsComponent;
    let fixture: ComponentFixture<ProductsComponent>;

    const httpServiceMock = vi.mockObject(HttpService.prototype);

    let routerNavigateSpy: Mock<(commands: readonly any[], extras?: NavigationExtras) => Promise<boolean>>;

    beforeEach(async () => {

        vi.resetAllMocks();

        await TestBed.configureTestingModule({
            imports: [ProductsComponent],
            providers: [
                provideRouter([]),
                {provide: HttpService, useValue: httpServiceMock}
            ]
        })
            .compileComponents();

        fixture = TestBed.createComponent(ProductsComponent);
        component = fixture.componentInstance;

        routerNavigateSpy = vi.spyOn(TestBed.inject(Router), 'navigate');
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
    });

    it('should navigate to ' + routeName.products_edit, () => {

        routerNavigateSpy.mockReturnValue(firstValueFrom(of(true)));

        component.products = [
            {id: 1, name: "product-1%&/+=", price: Big('123.45')},
            {id: 2, name: "product-2%&/+=", price: Big('678.90')}
        ] as GetProductsResponse[];

        const urlAppend = encodeURIComponent(window.btoa(JSON.stringify({
            id: 1,
            name: "product-1%&/+=",
            price: Big('123.45')
        })));

        component.onClick(0);

        expect(routerNavigateSpy).toHaveBeenCalledExactlyOnceWith(['/' + routeName.products_edit + '/' + urlAppend]);
    });
});

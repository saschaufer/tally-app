import {provideLocationMocks} from "@angular/common/testing";
import {Component} from "@angular/core";
import {ComponentFixture, TestBed} from '@angular/core/testing';
import {provideRouter} from "@angular/router";
import {RouterTestingHarness} from "@angular/router/testing";
import {firstValueFrom} from "rxjs";
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {AppComponent} from './app.component';
import {routeName} from "./app.routes";
import {AuthService} from "./services/auth.service";


@Component({selector: 'app-test', template: ''})
class A {
}

describe('AppComponent', () => {

    let component: AppComponent;
    let fixture: ComponentFixture<AppComponent>;
    let routerHarness: RouterTestingHarness;

    const authServiceMock = vi.mockObject(AuthService.prototype);

    beforeEach(async () => {

        vi.resetAllMocks();

        await TestBed.configureTestingModule({
            imports: [AppComponent],
            providers: [
                {provide: AuthService, useValue: authServiceMock},
                provideRouter([
                    {path: '', component: A},
                    {path: routeName.login, component: A},
                    {path: routeName.payments, component: A},
                    {path: routeName.payments_delete, component: A},
                    {path: routeName.payments_new, component: A},
                    {path: routeName.register, component: A},
                    {path: routeName.register_confirm, component: A},
                    {path: routeName.settings, component: A},
                    {path: routeName.products, component: A},
                    {path: routeName.products_new, component: A},
                    {path: routeName.products_edit, component: A},
                    {path: routeName.purchases, component: A},
                    {path: routeName.purchases_delete, component: A},
                    {path: routeName.purchases_new, component: A},
                    {path: routeName.qr, component: A},
                    {path: routeName.reset_password, component: A},
                    {path: routeName.users, component: A},
                ]),
                provideLocationMocks()
            ]
        }).compileComponents();

        fixture = TestBed.createComponent(AppComponent);
        component = fixture.componentInstance;

        routerHarness = await RouterTestingHarness.create('/');

        fixture.detectChanges();
    });

    it('should create the app', () => {
        expect(component).toBeTruthy();
    });

    it('should show the navbar', async () => {

        expect(await firstValueFrom(component.showNavBar)).toBeTruthy()

        await routerHarness.navigateByUrl('/').then();
        expect(await firstValueFrom(component.showNavBar)).toBeTruthy()

        await routerHarness.navigateByUrl('/' + routeName.login).then();
        expect(await firstValueFrom(component.showNavBar)).toBeFalsy();

        await routerHarness.navigateByUrl('/' + routeName.payments);
        expect(await firstValueFrom(component.showNavBar)).toBeTruthy();

        await routerHarness.navigateByUrl('/' + routeName.payments_delete);
        expect(await firstValueFrom(component.showNavBar)).toBeTruthy();

        await routerHarness.navigateByUrl('/' + routeName.payments_new);
        expect(await firstValueFrom(component.showNavBar)).toBeTruthy();

        await routerHarness.navigateByUrl('/' + routeName.register);
        expect(await firstValueFrom(component.showNavBar)).toBeFalsy();

        await routerHarness.navigateByUrl('/' + routeName.register_confirm);
        expect(await firstValueFrom(component.showNavBar)).toBeFalsy();

        await routerHarness.navigateByUrl('/' + routeName.settings);
        expect(await firstValueFrom(component.showNavBar)).toBeTruthy();

        await routerHarness.navigateByUrl('/' + routeName.products);
        expect(await firstValueFrom(component.showNavBar)).toBeTruthy();

        await routerHarness.navigateByUrl('/' + routeName.products_new);
        expect(await firstValueFrom(component.showNavBar)).toBeTruthy();

        await routerHarness.navigateByUrl('/' + routeName.products_edit);
        expect(await firstValueFrom(component.showNavBar)).toBeTruthy();

        await routerHarness.navigateByUrl('/' + routeName.purchases);
        expect(await firstValueFrom(component.showNavBar)).toBeTruthy();

        await routerHarness.navigateByUrl('/' + routeName.purchases_delete);
        expect(await firstValueFrom(component.showNavBar)).toBeTruthy();

        await routerHarness.navigateByUrl('/' + routeName.purchases_new);
        expect(await firstValueFrom(component.showNavBar)).toBeTruthy();

        await routerHarness.navigateByUrl('/' + routeName.qr);
        expect(await firstValueFrom(component.showNavBar)).toBeTruthy();

        await routerHarness.navigateByUrl('/' + routeName.reset_password);
        expect(await firstValueFrom(component.showNavBar)).toBeFalsy();

        await routerHarness.navigateByUrl('/' + routeName.users);
        expect(await firstValueFrom(component.showNavBar)).toBeTruthy();
    });
});

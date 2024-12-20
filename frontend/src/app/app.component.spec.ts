import {provideLocationMocks} from "@angular/common/testing";
import {NgZone} from "@angular/core";
import {ComponentFixture, fakeAsync, TestBed, tick} from '@angular/core/testing';
import {provideRouter} from "@angular/router";
import {RouterTestingHarness} from "@angular/router/testing";
import {MockComponent, MockProvider} from "ng-mocks";
import {AppComponent} from './app.component';
import {routeName} from "./app.routes";
import {PaymentDeleteComponent} from "./components/payments/payment-delete/payment-delete.component";
import {PaymentNewComponent} from "./components/payments/payment-new/payment-new.component";
import {PaymentsComponent} from "./components/payments/payments.component";
import {ProductEditComponent} from "./components/products/product-edit/product-edit.component";
import {ProductNewComponent} from "./components/products/product-new/product-new.component";
import {ProductsComponent} from "./components/products/products.component";
import {PurchaseDeleteComponent} from "./components/purchases/purchase-delete/purchase-delete.component";
import {PurchaseNewComponent} from "./components/purchases/purchase-new/purchase-new.component";
import {PurchasesComponent} from "./components/purchases/purchases.component";
import {QrComponent} from "./components/qr/qr.component";
import {RegisterConfirmComponent} from "./components/register/register-confirm/register-confirm.component";
import {RegisterComponent} from "./components/register/register.component";
import {ResetPasswordComponent} from "./components/reset-password/reset-password.component";
import {LoginDetailsComponent} from "./components/settings/login-details/login-details.component";
import {SettingsComponent} from "./components/settings/settings.component";
import {UsersComponent} from "./components/users/users.component";
import {AuthService} from "./services/auth.service";

describe('AppComponent', () => {

    let component: AppComponent;
    let fixture: ComponentFixture<AppComponent>;
    let routerHarness: RouterTestingHarness;
    let zone: NgZone;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [AppComponent],
            providers: [
                MockProvider(AuthService),
                provideRouter([
                    {path: '', component: MockComponent(LoginDetailsComponent)},
                    {path: routeName.login, component: MockComponent(LoginDetailsComponent)},
                    {path: routeName.payments, component: MockComponent(PaymentsComponent)},
                    {path: routeName.payments_delete, component: MockComponent(PaymentDeleteComponent)},
                    {path: routeName.payments_new, component: MockComponent(PaymentNewComponent)},
                    {path: routeName.register, component: MockComponent(RegisterComponent)},
                    {path: routeName.register_confirm, component: MockComponent(RegisterConfirmComponent)},
                    {path: routeName.settings, component: MockComponent(SettingsComponent)},
                    {path: routeName.products, component: MockComponent(ProductsComponent)},
                    {path: routeName.products_new, component: MockComponent(ProductNewComponent)},
                    {path: routeName.products_edit, component: MockComponent(ProductEditComponent)},
                    {path: routeName.purchases, component: MockComponent(PurchasesComponent)},
                    {path: routeName.purchases_delete, component: MockComponent(PurchaseDeleteComponent)},
                    {path: routeName.purchases_new, component: MockComponent(PurchaseNewComponent)},
                    {path: routeName.qr, component: MockComponent(QrComponent)},
                    {path: routeName.reset_password, component: MockComponent(ResetPasswordComponent)},
                    {path: routeName.users, component: MockComponent(UsersComponent)}
                ]),
                provideLocationMocks()
            ]
        }).compileComponents();

        fixture = TestBed.createComponent(AppComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();

        routerHarness = await RouterTestingHarness.create('/');
        zone = TestBed.inject(NgZone);
    });

    it('should create the app', () => {
        expect(component).toBeTruthy();
    });

    it('should show the navbar', fakeAsync(async () => {

        zone.run(() => {
            routerHarness.navigateByUrl('/');
            tick(1);
        });
        expect(component.showNavBar()).toBeTruthy()

        zone.run(() => {
            routerHarness.navigateByUrl('/' + routeName.login);
            tick(1);
        });
        expect(component.showNavBar()).toBeFalsy();

        zone.run(() => {
            routerHarness.navigateByUrl('/' + routeName.payments);
            tick(1);
        });
        expect(component.showNavBar()).toBeTruthy();

        zone.run(() => {
            routerHarness.navigateByUrl('/' + routeName.payments_delete);
            tick(1);
        });
        expect(component.showNavBar()).toBeTruthy();

        zone.run(() => {
            routerHarness.navigateByUrl('/' + routeName.payments_new);
            tick(1);
        });
        expect(component.showNavBar()).toBeTruthy();

        zone.run(() => {
            routerHarness.navigateByUrl('/' + routeName.register);
            tick(1);
        });
        expect(component.showNavBar()).toBeFalsy();

        zone.run(() => {
            routerHarness.navigateByUrl('/' + routeName.register_confirm);
            tick(1);
        });
        expect(component.showNavBar()).toBeFalsy();

        zone.run(() => {
            routerHarness.navigateByUrl('/' + routeName.settings);
            tick(1);
        });
        expect(component.showNavBar()).toBeTruthy();

        zone.run(() => {
            routerHarness.navigateByUrl('/' + routeName.products);
            tick(1);
        });
        expect(component.showNavBar()).toBeTruthy();

        zone.run(() => {
            routerHarness.navigateByUrl('/' + routeName.products_new);
            tick(1);
        });
        expect(component.showNavBar()).toBeTruthy();

        zone.run(() => {
            routerHarness.navigateByUrl('/' + routeName.products_edit);
            tick(1);
        });
        expect(component.showNavBar()).toBeTruthy();

        zone.run(() => {
            routerHarness.navigateByUrl('/' + routeName.purchases);
            tick(1);
        });
        expect(component.showNavBar()).toBeTruthy();

        zone.run(() => {
            routerHarness.navigateByUrl('/' + routeName.purchases_delete);
            tick(1);
        });
        expect(component.showNavBar()).toBeTruthy();

        zone.run(() => {
            routerHarness.navigateByUrl('/' + routeName.purchases_new);
            tick(1);
        });
        expect(component.showNavBar()).toBeTruthy();

        zone.run(() => {
            routerHarness.navigateByUrl('/' + routeName.qr);
            tick(1);
        });
        expect(component.showNavBar()).toBeTruthy();

        zone.run(() => {
            routerHarness.navigateByUrl('/' + routeName.reset_password);
            tick(1);
        });
        expect(component.showNavBar()).toBeFalsy();

        zone.run(() => {
            routerHarness.navigateByUrl('/' + routeName.users);
            tick(1);
        });
        expect(component.showNavBar()).toBeTruthy();
    }));
});

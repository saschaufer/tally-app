import {provideLocationMocks} from "@angular/common/testing";
import {NgZone} from "@angular/core";
import {ComponentFixture, fakeAsync, TestBed, tick} from '@angular/core/testing';
import {provideRouter} from "@angular/router";
import {RouterTestingHarness} from "@angular/router/testing";
import {MockComponent} from "ng-mocks";
import {AppComponent} from './app.component';
import {routeName} from "./app.routes";
import {RegisterComponent} from "./components/register/register.component";
import {LoginDetailsComponent} from "./components/settings/login-details/login-details.component";
import {SettingsComponent} from "./components/settings/settings.component";

describe('AppComponent', () => {

    let component: AppComponent;
    let fixture: ComponentFixture<AppComponent>;
    let routerHarness: RouterTestingHarness;
    let zone: NgZone;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [AppComponent],
            providers: [
                provideRouter([
                    {path: '', component: MockComponent(LoginDetailsComponent)},
                    {path: routeName.login, component: MockComponent(LoginDetailsComponent)},
                    {path: routeName.settings, component: MockComponent(SettingsComponent)},
                    {path: routeName.register, component: MockComponent(RegisterComponent)},
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
            routerHarness.navigateByUrl('/' + routeName.settings);
            tick(1);
        });
        expect(component.showNavBar()).toBeTruthy()

        zone.run(() => {
            routerHarness.navigateByUrl('/' + routeName.login);
            tick(1);
        });
        expect(component.showNavBar()).toBeFalsy();

        zone.run(() => {
            routerHarness.navigateByUrl('/' + routeName.register);
            tick(1);
        });
        expect(component.showNavBar()).toBeFalsy();
    }));
});

import {provideLocationMocks} from "@angular/common/testing";
import {ComponentFixture, TestBed} from '@angular/core/testing';
import {provideRouter, Router} from "@angular/router";
import {MockProvider} from "ng-mocks";
import {firstValueFrom, of, throwError} from "rxjs";
import {routeName} from "../../app.routes";
import {AuthService} from "../../services/auth.service";
import {HttpService} from "../../services/http.service";
import {LoginResponse} from "../../services/models/LoginResponse";

import {LoginComponent} from './login.component';
import Spy = jasmine.Spy;
import SpyObj = jasmine.SpyObj;

describe('LoginComponent', () => {

    let component: LoginComponent;
    let fixture: ComponentFixture<LoginComponent>;

    let authServiceSpy: SpyObj<AuthService>;
    let httpServiceSpy: SpyObj<HttpService>;

    let routerNavigateSpy: Spy;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [LoginComponent],
            providers: [
                MockProvider(AuthService),
                MockProvider(HttpService),
                provideRouter([]),
                provideLocationMocks()
            ]
        })
            .compileComponents();

        fixture = TestBed.createComponent(LoginComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();

        authServiceSpy = spyOnAllFunctions(TestBed.inject(AuthService));
        httpServiceSpy = spyOnAllFunctions(TestBed.inject(HttpService));

        routerNavigateSpy = spyOn(TestBed.inject(Router), 'navigate');
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    it('should navigate to ' + routeName.purchases_new, () => {

        httpServiceSpy.postLogin.and.callFake(() => of({
            jwt: 'my-jwt',
            secure: true
        } as LoginResponse));

        authServiceSpy.setJwt.and.returnValue(true);
        routerNavigateSpy.and.callFake(() => firstValueFrom(of(true)));

        component.loginForm.setValue({
            email: 'test-username@mail.com',
            password: 'test-password'
        })

        component.onSubmit();

        expect(httpServiceSpy.postLogin).toHaveBeenCalledOnceWith('test-username@mail.com', 'test-password');
        expect(authServiceSpy.setJwt).toHaveBeenCalledOnceWith('my-jwt', true);
        expect(routerNavigateSpy).toHaveBeenCalledOnceWith(['/' + routeName.purchases_new]);
    });

    it('should not navigate to ' + routeName.purchases_new + ' (email wrong)', () => {

        component.loginForm.controls.email.setErrors(['wrong']);
        component.loginForm.controls.password.patchValue('test-password');

        component.onSubmit();

        expect(httpServiceSpy.postLogin).not.toHaveBeenCalled();
        expect(authServiceSpy.setJwt).not.toHaveBeenCalled();
        expect(routerNavigateSpy).not.toHaveBeenCalled();
    });

    it('should not navigate to ' + routeName.purchases_new + ' (password wrong)', () => {

        component.loginForm.controls.email.patchValue('test-username@mail.com');
        component.loginForm.controls.password.setErrors(['wrong']);

        component.onSubmit();

        expect(httpServiceSpy.postLogin).not.toHaveBeenCalled();
        expect(authServiceSpy.setJwt).not.toHaveBeenCalled();
        expect(routerNavigateSpy).not.toHaveBeenCalled();
    });

    it('should not navigate to ' + routeName.purchases_new + ' (login failed)', () => {

        httpServiceSpy.postLogin.and.callFake(() =>
            throwError(() => 'Error on login')
        );

        component.loginForm.setValue({
            email: 'test-username@mail.com',
            password: 'test-password'
        })

        component.onSubmit();

        expect(httpServiceSpy.postLogin).toHaveBeenCalledOnceWith('test-username@mail.com', 'test-password');
        expect(authServiceSpy.setJwt).not.toHaveBeenCalled();
        expect(routerNavigateSpy).not.toHaveBeenCalled();
    });

    it('should not navigate to ' + routeName.purchases_new + ' (Jwt not set)', () => {

        httpServiceSpy.postLogin.and.callFake(() => of({
            jwt: 'my-jwt',
            secure: true
        } as LoginResponse));

        authServiceSpy.setJwt.and.returnValue(false);

        component.loginForm.setValue({
            email: 'test-username@mail.com',
            password: 'test-password'
        })

        component.onSubmit();

        expect(httpServiceSpy.postLogin).toHaveBeenCalledOnceWith('test-username@mail.com', 'test-password');
        expect(authServiceSpy.setJwt).toHaveBeenCalledOnceWith('my-jwt', true);
        expect(routerNavigateSpy).not.toHaveBeenCalled();
    });
});

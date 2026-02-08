import {provideLocationMocks} from "@angular/common/testing";
import {ComponentFixture, TestBed} from '@angular/core/testing';
import {NavigationExtras, provideRouter, Router} from "@angular/router";
import {Mock} from "@vitest/spy";
import {firstValueFrom, of, throwError} from "rxjs";
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {routeName} from "../../app.routes";
import {AuthService} from "../../services/auth.service";
import {HttpService} from "../../services/http.service";
import {LoginResponse} from "../../services/models/LoginResponse";

import {LoginComponent} from './login.component';

describe('LoginComponent', () => {

    let component: LoginComponent;
    let fixture: ComponentFixture<LoginComponent>;

    const authServiceMock = vi.mockObject(AuthService.prototype);
    const httpServiceMock = vi.mockObject(HttpService.prototype);

    let routerNavigateSpy: Mock<(commands: readonly any[], extras?: NavigationExtras) => Promise<boolean>>;

    beforeEach(async () => {

        vi.resetAllMocks();

        await TestBed.configureTestingModule({
            imports: [LoginComponent],
            providers: [
                {provide: AuthService, useValue: authServiceMock},
                {provide: HttpService, useValue: httpServiceMock},
                provideRouter([]),
                provideLocationMocks()
            ]
        })
            .compileComponents();

        fixture = TestBed.createComponent(LoginComponent);
        component = fixture.componentInstance;

        routerNavigateSpy = vi.spyOn(TestBed.inject(Router), 'navigate');

        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    it('should navigate to ' + routeName.purchases_new, () => {

        httpServiceMock.postLogin.mockReturnValue(of({
            jwt: 'my-jwt',
            secure: true
        } as LoginResponse));

        authServiceMock.setJwt.mockReturnValue(true);
        routerNavigateSpy.mockReturnValue(firstValueFrom(of(true)));

        component.loginForm.setValue({
            email: 'test-username@mail.com',
            password: 'test-password'
        })

        component.onSubmit();

        expect(httpServiceMock.postLogin).toHaveBeenCalledExactlyOnceWith('test-username@mail.com', 'test-password');
        expect(authServiceMock.setJwt).toHaveBeenCalledExactlyOnceWith('my-jwt', true);
        expect(routerNavigateSpy).toHaveBeenCalledExactlyOnceWith(['/' + routeName.purchases_new]);
    });

    it('should not navigate to ' + routeName.purchases_new + ' (email wrong)', () => {

        component.loginForm.controls.email.setErrors(['wrong']);
        component.loginForm.controls.password.patchValue('test-password');

        component.onSubmit();

        expect(httpServiceMock.postLogin).not.toHaveBeenCalled();
        expect(authServiceMock.setJwt).not.toHaveBeenCalled();
        expect(routerNavigateSpy).not.toHaveBeenCalled();
    });

    it('should not navigate to ' + routeName.purchases_new + ' (password wrong)', () => {

        component.loginForm.controls.email.patchValue('test-username@mail.com');
        component.loginForm.controls.password.setErrors(['wrong']);

        component.onSubmit();

        expect(httpServiceMock.postLogin).not.toHaveBeenCalled();
        expect(authServiceMock.setJwt).not.toHaveBeenCalled();
        expect(routerNavigateSpy).not.toHaveBeenCalled();
    });

    it('should not navigate to ' + routeName.purchases_new + ' (login failed)', () => {

        httpServiceMock.postLogin.mockReturnValue(
            throwError(() => 'Error on login')
        );

        component.loginForm.setValue({
            email: 'test-username@mail.com',
            password: 'test-password'
        })

        component.onSubmit();

        expect(httpServiceMock.postLogin).toHaveBeenCalledExactlyOnceWith('test-username@mail.com', 'test-password');
        expect(authServiceMock.setJwt).not.toHaveBeenCalled();
        expect(routerNavigateSpy).not.toHaveBeenCalled();
    });

    it('should not navigate to ' + routeName.purchases_new + ' (Jwt not set)', () => {

        httpServiceMock.postLogin.mockReturnValue(of({
            jwt: 'my-jwt',
            secure: true
        } as LoginResponse));

        authServiceMock.setJwt.mockReturnValue(false);

        component.loginForm.setValue({
            email: 'test-username@mail.com',
            password: 'test-password'
        })

        component.onSubmit();

        expect(httpServiceMock.postLogin).toHaveBeenCalledExactlyOnceWith('test-username@mail.com', 'test-password');
        expect(authServiceMock.setJwt).toHaveBeenCalledExactlyOnceWith('my-jwt', true);
        expect(routerNavigateSpy).not.toHaveBeenCalled();
    });
});

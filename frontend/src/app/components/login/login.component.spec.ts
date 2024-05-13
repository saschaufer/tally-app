import {ComponentFixture, TestBed} from '@angular/core/testing';
import {Router} from "@angular/router";
import {MockProvider} from "ng-mocks";
import {firstValueFrom, of, throwError} from "rxjs";
import {routeName} from "../../app.routes";
import {AuthService} from "../../services/auth.service";
import {HttpService} from "../../services/http.service";
import {LoginResponse} from "../../services/models/LoginResponse";

import {LoginComponent} from './login.component';
import SpyObj = jasmine.SpyObj;

describe('LoginComponent', () => {

    let component: LoginComponent;
    let fixture: ComponentFixture<LoginComponent>;

    let routerSpy: SpyObj<Router>;
    let authServiceSpy: SpyObj<AuthService>;
    let httpServiceSpy: SpyObj<HttpService>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [LoginComponent],
            providers: [
                MockProvider(AuthService),
                MockProvider(HttpService),
                MockProvider(Router)
            ]
        })
            .compileComponents();

        fixture = TestBed.createComponent(LoginComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();

        routerSpy = spyOnAllFunctions(TestBed.inject(Router));
        authServiceSpy = spyOnAllFunctions(TestBed.inject(AuthService));
        httpServiceSpy = spyOnAllFunctions(TestBed.inject(HttpService));
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    it('should navigate to ' + routeName.settings, () => {

        httpServiceSpy.postLogin.and.callFake(() => of({
            jwt: 'my-jwt',
            secure: true
        } as LoginResponse));

        authServiceSpy.setJwt.and.returnValue(true);
        routerSpy.navigate.and.callFake(() => firstValueFrom(of(true)));

        component.loginForm.setValue({
            username: 'test-username',
            password: 'test-password'
        })

        component.onSubmit();

        expect(httpServiceSpy.postLogin).toHaveBeenCalledOnceWith('test-username', 'test-password');
        expect(authServiceSpy.setJwt).toHaveBeenCalledOnceWith('my-jwt', true);
        expect(routerSpy.navigate).toHaveBeenCalledOnceWith(['/' + routeName.settings]);
    });

    it('should not navigate to ' + routeName.settings + ' (username wrong)', () => {

        component.loginForm.controls.username.setErrors(['wrong']);
        component.loginForm.controls.password.patchValue('test-password');

        component.onSubmit();

        expect(httpServiceSpy.postLogin).not.toHaveBeenCalled();
        expect(authServiceSpy.setJwt).not.toHaveBeenCalled();
        expect(routerSpy.navigate).not.toHaveBeenCalled();
    });

    it('should not navigate to ' + routeName.settings + ' (password wrong)', () => {

        component.loginForm.controls.username.patchValue('test-username');
        component.loginForm.controls.password.setErrors(['wrong']);

        component.onSubmit();

        expect(httpServiceSpy.postLogin).not.toHaveBeenCalled();
        expect(authServiceSpy.setJwt).not.toHaveBeenCalled();
        expect(routerSpy.navigate).not.toHaveBeenCalled();
    });

    it('should not navigate to ' + routeName.settings + ' (login failed)', () => {

        httpServiceSpy.postLogin.and.callFake(() =>
            throwError(() => 'Error on login')
        );

        component.loginForm.setValue({
            username: 'test-username',
            password: 'test-password'
        })

        component.onSubmit();

        expect(httpServiceSpy.postLogin).toHaveBeenCalledOnceWith('test-username', 'test-password');
        expect(authServiceSpy.setJwt).not.toHaveBeenCalled();
        expect(routerSpy.navigate).not.toHaveBeenCalled();
    });

    it('should not navigate to ' + routeName.settings + ' (Jwt not set)', () => {

        httpServiceSpy.postLogin.and.callFake(() => of({
            jwt: 'my-jwt',
            secure: true
        } as LoginResponse));

        authServiceSpy.setJwt.and.returnValue(false);

        component.loginForm.setValue({
            username: 'test-username',
            password: 'test-password'
        })

        component.onSubmit();

        expect(httpServiceSpy.postLogin).toHaveBeenCalledOnceWith('test-username', 'test-password');
        expect(authServiceSpy.setJwt).toHaveBeenCalledOnceWith('my-jwt', true);
        expect(routerSpy.navigate).not.toHaveBeenCalled();
    });
});

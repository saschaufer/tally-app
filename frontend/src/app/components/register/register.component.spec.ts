import {provideLocationMocks} from "@angular/common/testing";
import {ComponentFixture, TestBed} from '@angular/core/testing';
import {provideRouter, Router} from "@angular/router";
import {MockProvider} from "ng-mocks";
import {firstValueFrom, of, throwError} from "rxjs";
import {routeName} from "../../app.routes";
import {HttpService} from "../../services/http.service";

import {RegisterComponent} from './register.component';
import Spy = jasmine.Spy;
import SpyObj = jasmine.SpyObj;

describe('RegisterComponent', () => {

    let component: RegisterComponent;
    let fixture: ComponentFixture<RegisterComponent>;

    let httpServiceSpy: SpyObj<HttpService>;

    let routerNavigateSpy: Spy;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [RegisterComponent],
            providers: [
                MockProvider(HttpService),
                provideRouter([]),
                provideLocationMocks()
            ]
        })
            .compileComponents();

        fixture = TestBed.createComponent(RegisterComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();

        httpServiceSpy = spyOnAllFunctions(TestBed.inject(HttpService));

        routerNavigateSpy = spyOn(TestBed.inject(Router), 'navigate');
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    it('should navigate to ' + routeName.login, () => {

        httpServiceSpy.postRegisterNewUser.and.callFake(() => of(undefined));

        routerNavigateSpy.and.callFake(() => firstValueFrom(of(true)));

        component.registerForm.setValue({
            username: 'test-username',
            password: 'test-password',
            passwordRepeat: 'test-password',
            invitationCode: 'test-invitation-code',
        })

        component.onSubmit();

        expect(httpServiceSpy.postRegisterNewUser).toHaveBeenCalledOnceWith('test-username', 'test-password', 'test-invitation-code');
        expect(routerNavigateSpy).toHaveBeenCalledOnceWith(['/' + routeName.login]);
    });

    it('should not navigate to ' + routeName.login + ' (username wrong)', () => {

        component.registerForm.controls.username.setErrors(['wrong']);
        component.registerForm.controls.password.patchValue('test-password');
        component.registerForm.controls.passwordRepeat.patchValue('test-password');
        component.registerForm.controls.invitationCode.patchValue('test-invitation-code');

        component.onSubmit();

        expect(httpServiceSpy.postRegisterNewUser).not.toHaveBeenCalled();
        expect(routerNavigateSpy).not.toHaveBeenCalled();
    });

    it('should not navigate to ' + routeName.login + ' (password wrong)', () => {

        component.registerForm.controls.username.patchValue('test-username');
        component.registerForm.controls.password.setErrors(['wrong']);
        component.registerForm.controls.passwordRepeat.patchValue('test-password');
        component.registerForm.controls.invitationCode.patchValue('test-invitation-code');

        component.onSubmit();

        expect(httpServiceSpy.postRegisterNewUser).not.toHaveBeenCalled();
        expect(routerNavigateSpy).not.toHaveBeenCalled();
    });

    it('should not navigate to ' + routeName.login + ' (password-repeat wrong)', () => {

        component.registerForm.controls.username.patchValue('test-username');
        component.registerForm.controls.password.patchValue('test-password');
        component.registerForm.controls.passwordRepeat.setErrors(['wrong']);
        component.registerForm.controls.invitationCode.patchValue('test-invitation-code');

        component.onSubmit();

        expect(httpServiceSpy.postRegisterNewUser).not.toHaveBeenCalled();
        expect(routerNavigateSpy).not.toHaveBeenCalled();
    });

    it('should not navigate to ' + routeName.login + ' (invitation-code wrong)', () => {

        component.registerForm.controls.username.patchValue('test-username');
        component.registerForm.controls.password.patchValue('test-password');
        component.registerForm.controls.passwordRepeat.patchValue('test-password');
        component.registerForm.controls.invitationCode.setErrors(['wrong']);

        component.onSubmit();

        expect(httpServiceSpy.postRegisterNewUser).not.toHaveBeenCalled();
        expect(routerNavigateSpy).not.toHaveBeenCalled();
    });

    it('should not navigate to ' + routeName.login + ' (password and password-repeat unequal)', () => {

        component.registerForm.controls.username.patchValue('test-username');
        component.registerForm.controls.password.patchValue('test-password');
        component.registerForm.controls.passwordRepeat.patchValue('test-password-unequal');
        component.registerForm.controls.invitationCode.patchValue('test-invitation-code');

        component.onSubmit();

        expect(httpServiceSpy.postRegisterNewUser).not.toHaveBeenCalled();
        expect(routerNavigateSpy).not.toHaveBeenCalled();
    });

    it('should not navigate to ' + routeName.login + ' (register failed)', () => {

        httpServiceSpy.postRegisterNewUser.and.callFake(() =>
            throwError(() => 'Error on register')
        );

        component.registerForm.setValue({
            username: 'test-username',
            password: 'test-password',
            passwordRepeat: 'test-password',
            invitationCode: 'test-invitation-code',
        })

        component.onSubmit();

        expect(httpServiceSpy.postRegisterNewUser).toHaveBeenCalledOnceWith('test-username', 'test-password', 'test-invitation-code');
        expect(routerNavigateSpy).not.toHaveBeenCalled();
    });
});

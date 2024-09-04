import {ComponentFixture, TestBed} from '@angular/core/testing';
import {provideRouter} from "@angular/router";
import {MockProvider} from "ng-mocks";
import {of, throwError} from "rxjs";
import {routeName} from "../../app.routes";
import {HttpService} from "../../services/http.service";

import {RegisterComponent} from './register.component';
import SpyObj = jasmine.SpyObj;

describe('RegisterComponent', () => {

    let component: RegisterComponent;
    let fixture: ComponentFixture<RegisterComponent>;

    let httpServiceSpy: SpyObj<HttpService>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [RegisterComponent],
            providers: [
                MockProvider(HttpService),
                provideRouter([])
            ]
        })
            .compileComponents();

        fixture = TestBed.createComponent(RegisterComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();

        httpServiceSpy = spyOnAllFunctions(TestBed.inject(HttpService));
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    it('should switch to email sent', () => {

        httpServiceSpy.postRegisterNewUser.and.callFake(() => of(undefined));

        component.registerForm.setValue({
            email: 'test-username@mail.com',
            password: 'test-password',
            passwordRepeat: 'test-password',
            invitationCode: 'test-invitation-code',
        })

        expect(component.email).toBe('');
        expect(component.emailSent).toBe(false);

        component.onSubmit();

        expect(httpServiceSpy.postRegisterNewUser).toHaveBeenCalledOnceWith('test-username@mail.com', 'test-password', 'test-invitation-code');

        expect(component.email).toBe('test-username@mail.com');
        expect(component.emailSent).toBe(true);
    });

    it('should not switch to email sent (email wrong)', () => {

        component.registerForm.controls.email.setErrors(['wrong']);
        component.registerForm.controls.password.patchValue('test-password');
        component.registerForm.controls.passwordRepeat.patchValue('test-password');
        component.registerForm.controls.invitationCode.patchValue('test-invitation-code');

        expect(component.email).toBe('');
        expect(component.emailSent).toBe(false);

        component.onSubmit();

        expect(httpServiceSpy.postRegisterNewUser).not.toHaveBeenCalled();

        expect(component.email).toBe('');
        expect(component.emailSent).toBe(false);
    });

    it('should not navigate to ' + routeName.login + ' (password wrong)', () => {

        component.registerForm.controls.email.patchValue('test-username@mail.com');
        component.registerForm.controls.password.setErrors(['wrong']);
        component.registerForm.controls.passwordRepeat.patchValue('test-password');
        component.registerForm.controls.invitationCode.patchValue('test-invitation-code');

        expect(component.email).toBe('');
        expect(component.emailSent).toBe(false);

        component.onSubmit();

        expect(httpServiceSpy.postRegisterNewUser).not.toHaveBeenCalled();

        expect(component.email).toBe('');
        expect(component.emailSent).toBe(false);
    });

    it('should not navigate to ' + routeName.login + ' (password-repeat wrong)', () => {

        component.registerForm.controls.email.patchValue('test-username@mail.com');
        component.registerForm.controls.password.patchValue('test-password');
        component.registerForm.controls.passwordRepeat.setErrors(['wrong']);
        component.registerForm.controls.invitationCode.patchValue('test-invitation-code');

        expect(component.email).toBe('');
        expect(component.emailSent).toBe(false);

        component.onSubmit();

        expect(httpServiceSpy.postRegisterNewUser).not.toHaveBeenCalled();

        expect(component.email).toBe('');
        expect(component.emailSent).toBe(false);
    });

    it('should not navigate to ' + routeName.login + ' (invitation-code wrong)', () => {

        component.registerForm.controls.email.patchValue('test-username@mail.com');
        component.registerForm.controls.password.patchValue('test-password');
        component.registerForm.controls.passwordRepeat.patchValue('test-password');
        component.registerForm.controls.invitationCode.setErrors(['wrong']);

        expect(component.email).toBe('');
        expect(component.emailSent).toBe(false);

        component.onSubmit();

        expect(httpServiceSpy.postRegisterNewUser).not.toHaveBeenCalled();

        expect(component.email).toBe('');
        expect(component.emailSent).toBe(false);
    });

    it('should not navigate to ' + routeName.login + ' (password and password-repeat unequal)', () => {

        component.registerForm.controls.email.patchValue('test-username@mail.com');
        component.registerForm.controls.password.patchValue('test-password');
        component.registerForm.controls.passwordRepeat.patchValue('test-password-unequal');
        component.registerForm.controls.invitationCode.patchValue('test-invitation-code');

        expect(component.email).toBe('');
        expect(component.emailSent).toBe(false);

        component.onSubmit();

        expect(httpServiceSpy.postRegisterNewUser).not.toHaveBeenCalled();

        expect(component.email).toBe('');
        expect(component.emailSent).toBe(false);
    });

    it('should not navigate to ' + routeName.login + ' (register failed)', () => {

        httpServiceSpy.postRegisterNewUser.and.callFake(() =>
            throwError(() => 'Error on register')
        );

        component.registerForm.setValue({
            email: 'test-username@mail.com',
            password: 'test-password',
            passwordRepeat: 'test-password',
            invitationCode: 'test-invitation-code',
        })

        expect(component.email).toBe('');
        expect(component.emailSent).toBe(false);

        component.onSubmit();

        expect(httpServiceSpy.postRegisterNewUser).toHaveBeenCalledOnceWith('test-username@mail.com', 'test-password', 'test-invitation-code');

        expect(component.email).toBe('');
        expect(component.emailSent).toBe(false);
    });
});

import {ComponentFixture, TestBed} from '@angular/core/testing';
import {provideRouter} from "@angular/router";
import {of, throwError} from "rxjs";
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {HttpService} from "../../services/http.service";

import {RegisterComponent} from './register.component';

describe('RegisterComponent', () => {

    let component: RegisterComponent;
    let fixture: ComponentFixture<RegisterComponent>;

    const httpServiceMock = vi.mockObject(HttpService.prototype);

    beforeEach(async () => {

        vi.resetAllMocks();

        await TestBed.configureTestingModule({
            imports: [RegisterComponent],
            providers: [
                provideRouter([]),
                {provide: HttpService, useValue: httpServiceMock}
            ]
        })
            .compileComponents();

        fixture = TestBed.createComponent(RegisterComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    it('should switch to email sent', () => {

        httpServiceMock.postRegisterNewUser.mockReturnValue(of(undefined));

        component.registerForm.setValue({
            email: 'test-username@mail.com',
            password: 'test-password',
            passwordRepeat: 'test-password',
            invitationCode: 'test-invitation-code',
        })

        expect(component.email).toBe('');
        expect(component.emailSent).toBe(false);

        component.onSubmit();

        expect(httpServiceMock.postRegisterNewUser).toHaveBeenCalledExactlyOnceWith('test-username@mail.com', 'test-password', 'test-invitation-code');

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

        expect(httpServiceMock.postRegisterNewUser).not.toHaveBeenCalled();

        expect(component.email).toBe('');
        expect(component.emailSent).toBe(false);
    });

    it('should not switch to email sent (password wrong)', () => {

        component.registerForm.controls.email.patchValue('test-username@mail.com');
        component.registerForm.controls.password.setErrors(['wrong']);
        component.registerForm.controls.passwordRepeat.patchValue('test-password');
        component.registerForm.controls.invitationCode.patchValue('test-invitation-code');

        expect(component.email).toBe('');
        expect(component.emailSent).toBe(false);

        component.onSubmit();

        expect(httpServiceMock.postRegisterNewUser).not.toHaveBeenCalled();

        expect(component.email).toBe('');
        expect(component.emailSent).toBe(false);
    });

    it('should not switch to email sent (password-repeat wrong)', () => {

        component.registerForm.controls.email.patchValue('test-username@mail.com');
        component.registerForm.controls.password.patchValue('test-password');
        component.registerForm.controls.passwordRepeat.setErrors(['wrong']);
        component.registerForm.controls.invitationCode.patchValue('test-invitation-code');

        expect(component.email).toBe('');
        expect(component.emailSent).toBe(false);

        component.onSubmit();

        expect(httpServiceMock.postRegisterNewUser).not.toHaveBeenCalled();

        expect(component.email).toBe('');
        expect(component.emailSent).toBe(false);
    });

    it('should not switch to email sent (invitation-code wrong)', () => {

        component.registerForm.controls.email.patchValue('test-username@mail.com');
        component.registerForm.controls.password.patchValue('test-password');
        component.registerForm.controls.passwordRepeat.patchValue('test-password');
        component.registerForm.controls.invitationCode.setErrors(['wrong']);

        expect(component.email).toBe('');
        expect(component.emailSent).toBe(false);

        component.onSubmit();

        expect(httpServiceMock.postRegisterNewUser).not.toHaveBeenCalled();

        expect(component.email).toBe('');
        expect(component.emailSent).toBe(false);
    });

    it('should not switch to email sent (password and password-repeat unequal)', () => {

        component.registerForm.controls.email.patchValue('test-username@mail.com');
        component.registerForm.controls.password.patchValue('test-password');
        component.registerForm.controls.passwordRepeat.patchValue('test-password-unequal');
        component.registerForm.controls.invitationCode.patchValue('test-invitation-code');

        expect(component.email).toBe('');
        expect(component.emailSent).toBe(false);

        component.onSubmit();

        expect(httpServiceMock.postRegisterNewUser).not.toHaveBeenCalled();

        expect(component.email).toBe('');
        expect(component.emailSent).toBe(false);
    });

    it('should not switch to email sent (register failed)', () => {

        httpServiceMock.postRegisterNewUser.mockReturnValue(
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

        expect(httpServiceMock.postRegisterNewUser).toHaveBeenCalledExactlyOnceWith('test-username@mail.com', 'test-password', 'test-invitation-code');

        expect(component.email).toBe('');
        expect(component.emailSent).toBe(false);
    });
});

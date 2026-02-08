import {ComponentFixture, TestBed} from '@angular/core/testing';
import {provideRouter} from "@angular/router";
import {of, throwError} from "rxjs";
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {HttpService} from "../../services/http.service";

import {ResetPasswordComponent} from './reset-password.component';

describe('ResetPasswordComponent', () => {

    let component: ResetPasswordComponent;
    let fixture: ComponentFixture<ResetPasswordComponent>;

    const httpServiceMock = vi.mockObject(HttpService.prototype);

    beforeEach(async () => {

        vi.resetAllMocks();

        await TestBed.configureTestingModule({
            imports: [ResetPasswordComponent],
            providers: [
                provideRouter([]),
                {provide: HttpService, useValue: httpServiceMock}
            ]
        })
            .compileComponents();

        fixture = TestBed.createComponent(ResetPasswordComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    it('should switch to email sent', () => {

        httpServiceMock.postResetPassword.mockReturnValue(of(undefined));

        component.resetPasswordForm.setValue({
            email: 'test-username@mail.com'
        })

        expect(component.email).toBe('');
        expect(component.emailSent).toBe(false);

        component.onSubmit();

        expect(httpServiceMock.postResetPassword).toHaveBeenCalledExactlyOnceWith('test-username@mail.com');

        expect(component.email).toBe('test-username@mail.com');
        expect(component.emailSent).toBe(true);
    });

    it('should not switch to email sent (email wrong)', () => {

        component.resetPasswordForm.controls.email.setErrors(['wrong']);

        expect(component.email).toBe('');
        expect(component.emailSent).toBe(false);

        component.onSubmit();

        expect(httpServiceMock.postResetPassword).not.toHaveBeenCalled();

        expect(component.email).toBe('');
        expect(component.emailSent).toBe(false);
    });

    it('should not switch to email sent (register failed)', () => {

        httpServiceMock.postResetPassword.mockReturnValue(
            throwError(() => 'Error on resetting password')
        );

        component.resetPasswordForm.setValue({
            email: 'test-username@mail.com'
        })

        expect(component.email).toBe('');
        expect(component.emailSent).toBe(false);

        component.onSubmit();

        expect(httpServiceMock.postResetPassword).toHaveBeenCalledExactlyOnceWith('test-username@mail.com');

        expect(component.email).toBe('');
        expect(component.emailSent).toBe(false);
    });
});

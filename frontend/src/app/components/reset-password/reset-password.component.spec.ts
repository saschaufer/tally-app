import {ComponentFixture, TestBed} from '@angular/core/testing';
import {provideRouter} from "@angular/router";
import {MockProvider} from "ng-mocks";
import {of, throwError} from "rxjs";
import {HttpService} from "../../services/http.service";

import {ResetPasswordComponent} from './reset-password.component';
import SpyObj = jasmine.SpyObj;

describe('ResetPasswordComponent', () => {

    let component: ResetPasswordComponent;
    let fixture: ComponentFixture<ResetPasswordComponent>;

    let httpServiceSpy: SpyObj<HttpService>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [ResetPasswordComponent],
            providers: [
                MockProvider(HttpService),
                provideRouter([])
            ]
        })
            .compileComponents();

        fixture = TestBed.createComponent(ResetPasswordComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();

        httpServiceSpy = spyOnAllFunctions(TestBed.inject(HttpService));
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    it('should switch to email sent', () => {

        httpServiceSpy.postResetPassword.and.callFake(() => of(undefined));

        component.resetPasswordForm.setValue({
            email: 'test-username@mail.com'
        })

        expect(component.email).toBe('');
        expect(component.emailSent).toBe(false);

        component.onSubmit();

        expect(httpServiceSpy.postResetPassword).toHaveBeenCalledOnceWith('test-username@mail.com');

        expect(component.email).toBe('test-username@mail.com');
        expect(component.emailSent).toBe(true);
    });

    it('should not switch to email sent (email wrong)', () => {

        component.resetPasswordForm.controls.email.setErrors(['wrong']);

        expect(component.email).toBe('');
        expect(component.emailSent).toBe(false);

        component.onSubmit();

        expect(httpServiceSpy.postResetPassword).not.toHaveBeenCalled();

        expect(component.email).toBe('');
        expect(component.emailSent).toBe(false);
    });

    it('should not switch to email sent (register failed)', () => {

        httpServiceSpy.postResetPassword.and.callFake(() =>
            throwError(() => 'Error on resetting password')
        );

        component.resetPasswordForm.setValue({
            email: 'test-username@mail.com'
        })

        expect(component.email).toBe('');
        expect(component.emailSent).toBe(false);

        component.onSubmit();

        expect(httpServiceSpy.postResetPassword).toHaveBeenCalledOnceWith('test-username@mail.com');

        expect(component.email).toBe('');
        expect(component.emailSent).toBe(false);
    });
});

import {ComponentFixture, TestBed} from '@angular/core/testing';
import {of, throwError} from "rxjs";
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {HttpService} from "../../../services/http.service";

import {ChangeUserDetailsComponent} from './change-user-details.component';

describe('ChangeUserDetailsComponent', () => {

    let component: ChangeUserDetailsComponent;
    let fixture: ComponentFixture<ChangeUserDetailsComponent>;

    const httpServiceMock = vi.mockObject(HttpService.prototype);

    beforeEach(async () => {

        vi.resetAllMocks();

        await TestBed.configureTestingModule({
            imports: [ChangeUserDetailsComponent],
            providers: [{provide: HttpService, useValue: httpServiceMock}]
        })
            .compileComponents();

        fixture = TestBed.createComponent(ChangeUserDetailsComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    it('should change the password', () => {

        httpServiceMock.postChangePassword.mockReturnValue(of(undefined));

        component.changePasswordForm.setValue({
            password: 'test-password',
            passwordRepeat: 'test-password'
        });

        component.onSubmit();

        expect(httpServiceMock.postChangePassword).toHaveBeenCalledExactlyOnceWith('test-password');
    });

    it('should not change the password (password wrong)', () => {

        httpServiceMock.postChangePassword.mockReturnValue(of(undefined));

        component.changePasswordForm.controls.password.setErrors(['wrong']);
        component.changePasswordForm.controls.passwordRepeat.patchValue('test-password');

        component.onSubmit();

        expect(httpServiceMock.postChangePassword).not.toHaveBeenCalled();
    });

    it('should not change the password (password-repeat wrong)', () => {

        httpServiceMock.postChangePassword.mockReturnValue(of(undefined));

        component.changePasswordForm.controls.password.patchValue('test-password');
        component.changePasswordForm.controls.passwordRepeat.setErrors(['wrong']);

        component.onSubmit();

        expect(httpServiceMock.postChangePassword).not.toHaveBeenCalled();
    });

    it('should not change the password (password and password-repeat unequal)', () => {

        httpServiceMock.postChangePassword.mockReturnValue(of(undefined));

        component.changePasswordForm.controls.password.patchValue('test-password');
        component.changePasswordForm.controls.passwordRepeat.patchValue('test-password-unequal');

        component.onSubmit();

        expect(httpServiceMock.postChangePassword).not.toHaveBeenCalled();
    });

    it('should not change the password (change password failed)', () => {

        httpServiceMock.postChangePassword.mockReturnValue(
            throwError(() => 'Error on change password')
        );

        component.changePasswordForm.controls.password.patchValue('test-password');
        component.changePasswordForm.controls.passwordRepeat.patchValue('test-password');

        component.onSubmit();

        expect(httpServiceMock.postChangePassword).toHaveBeenCalledExactlyOnceWith('test-password');
    });
});

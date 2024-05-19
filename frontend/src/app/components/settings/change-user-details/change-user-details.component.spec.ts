import {ComponentFixture, TestBed} from '@angular/core/testing';
import {MockProvider} from "ng-mocks";
import {of, throwError} from "rxjs";
import {HttpService} from "../../../services/http.service";

import {ChangeUserDetailsComponent} from './change-user-details.component';
import SpyObj = jasmine.SpyObj;

describe('ChangeUserDetailsComponent', () => {

    let component: ChangeUserDetailsComponent;
    let fixture: ComponentFixture<ChangeUserDetailsComponent>;

    let httpServiceSpy: SpyObj<HttpService>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [ChangeUserDetailsComponent],
            providers: [MockProvider(HttpService)]
        })
            .compileComponents();

        fixture = TestBed.createComponent(ChangeUserDetailsComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();

        httpServiceSpy = spyOnAllFunctions(TestBed.inject(HttpService));
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    it('should change the password', () => {

        httpServiceSpy.postChangePassword.and.callFake(() => of(undefined));

        component.changePasswordForm.setValue({
            password: 'test-password',
            passwordRepeat: 'test-password'
        });

        component.onSubmit();

        expect(httpServiceSpy.postChangePassword).toHaveBeenCalledOnceWith('test-password');
    });

    it('should not change the password (password wrong)', () => {

        httpServiceSpy.postChangePassword.and.callFake(() => of(undefined));

        component.changePasswordForm.controls.password.setErrors(['wrong']);
        component.changePasswordForm.controls.passwordRepeat.patchValue('test-password');

        component.onSubmit();

        expect(httpServiceSpy.postChangePassword).not.toHaveBeenCalled();
    });

    it('should not change the password (password-repeat wrong)', () => {

        httpServiceSpy.postChangePassword.and.callFake(() => of(undefined));

        component.changePasswordForm.controls.password.patchValue('test-password');
        component.changePasswordForm.controls.passwordRepeat.setErrors(['wrong']);

        component.onSubmit();

        expect(httpServiceSpy.postChangePassword).not.toHaveBeenCalled();
    });

    it('should not change the password (password and password-repeat unequal)', () => {

        httpServiceSpy.postChangePassword.and.callFake(() => of(undefined));

        component.changePasswordForm.controls.password.patchValue('test-password');
        component.changePasswordForm.controls.passwordRepeat.patchValue('test-password-unequal');

        component.onSubmit();

        expect(httpServiceSpy.postChangePassword).not.toHaveBeenCalled();
    });

    it('should not change the password (change password failed)', () => {

        httpServiceSpy.postChangePassword.and.callFake(() =>
            throwError(() => 'Error on change password')
        );

        component.changePasswordForm.controls.password.patchValue('test-password');
        component.changePasswordForm.controls.passwordRepeat.patchValue('test-password');

        component.onSubmit();

        expect(httpServiceSpy.postChangePassword).toHaveBeenCalledOnceWith('test-password');
    });
});

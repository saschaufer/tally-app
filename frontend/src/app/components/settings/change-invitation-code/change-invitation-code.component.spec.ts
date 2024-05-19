import {ComponentFixture, TestBed} from '@angular/core/testing';
import {MockProvider} from "ng-mocks";
import {of, throwError} from "rxjs";
import {HttpService} from "../../../services/http.service";

import {ChangeInvitationCodeComponent} from './change-invitation-code.component';
import SpyObj = jasmine.SpyObj;

describe('ChangeInvitationCodeComponent', () => {

    let component: ChangeInvitationCodeComponent;
    let fixture: ComponentFixture<ChangeInvitationCodeComponent>;

    let httpServiceSpy: SpyObj<HttpService>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [ChangeInvitationCodeComponent],
            providers: [MockProvider(HttpService)]
        })
            .compileComponents();

        fixture = TestBed.createComponent(ChangeInvitationCodeComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();

        httpServiceSpy = spyOnAllFunctions(TestBed.inject(HttpService));
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    it('should change the invitation-code', () => {

        httpServiceSpy.postChangeInvitationCode.and.callFake(() => of(undefined));

        component.changeInvitationCodeForm.setValue({
            invitationCode: 'test-invitation-code',
            invitationCodeRepeat: 'test-invitation-code'
        });

        component.onSubmit();

        expect(httpServiceSpy.postChangeInvitationCode).toHaveBeenCalledOnceWith('test-invitation-code');
    });

    it('should not change the invitation-code (invitation-code wrong)', () => {

        httpServiceSpy.postChangeInvitationCode.and.callFake(() => of(undefined));

        component.changeInvitationCodeForm.controls.invitationCode.setErrors(['wrong']);
        component.changeInvitationCodeForm.controls.invitationCodeRepeat.patchValue('test-invitation-code');

        component.onSubmit();

        expect(httpServiceSpy.postChangeInvitationCode).not.toHaveBeenCalled();
    });

    it('should not change the invitation-code (invitation-code-repeat wrong)', () => {

        httpServiceSpy.postChangeInvitationCode.and.callFake(() => of(undefined));

        component.changeInvitationCodeForm.controls.invitationCode.patchValue('test-invitation-code');
        component.changeInvitationCodeForm.controls.invitationCodeRepeat.setErrors(['wrong']);

        component.onSubmit();

        expect(httpServiceSpy.postChangeInvitationCode).not.toHaveBeenCalled();
    });

    it('should not change the invitation-code (invitation-code and invitation-code-repeat unequal)', () => {

        httpServiceSpy.postChangeInvitationCode.and.callFake(() => of(undefined));

        component.changeInvitationCodeForm.controls.invitationCode.patchValue('test-invitation-code');
        component.changeInvitationCodeForm.controls.invitationCodeRepeat.patchValue('test-invitation-code-unequal');

        component.onSubmit();

        expect(httpServiceSpy.postChangeInvitationCode).not.toHaveBeenCalled();
    });

    it('should not change the invitation-code (change invitation-code failed)', () => {

        httpServiceSpy.postChangeInvitationCode.and.callFake(() =>
            throwError(() => 'Error on change invitation-code')
        );

        component.changeInvitationCodeForm.controls.invitationCode.patchValue('test-invitation-code');
        component.changeInvitationCodeForm.controls.invitationCodeRepeat.patchValue('test-invitation-code');

        component.onSubmit();

        expect(httpServiceSpy.postChangeInvitationCode).toHaveBeenCalledOnceWith('test-invitation-code');
    });
});

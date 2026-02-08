import {ComponentFixture, TestBed} from '@angular/core/testing';
import {of, throwError} from "rxjs";
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {HttpService} from "../../../services/http.service";

import {ChangeInvitationCodeComponent} from './change-invitation-code.component';

describe('ChangeInvitationCodeComponent', () => {

    let component: ChangeInvitationCodeComponent;
    let fixture: ComponentFixture<ChangeInvitationCodeComponent>;

    const httpServiceMock = vi.mockObject(HttpService.prototype);

    beforeEach(async () => {

        vi.resetAllMocks();

        await TestBed.configureTestingModule({
            imports: [ChangeInvitationCodeComponent],
            providers: [{provide: HttpService, useValue: httpServiceMock}]
        })
            .compileComponents();

        fixture = TestBed.createComponent(ChangeInvitationCodeComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    it('should change the invitation-code', () => {

        httpServiceMock.postChangeInvitationCode.mockReturnValue(of(undefined));

        component.changeInvitationCodeForm.setValue({
            invitationCode: 'test-invitation-code',
            invitationCodeRepeat: 'test-invitation-code'
        });

        component.onSubmit();

        expect(httpServiceMock.postChangeInvitationCode).toHaveBeenCalledExactlyOnceWith('test-invitation-code');
    });

    it('should not change the invitation-code (invitation-code wrong)', () => {

        httpServiceMock.postChangeInvitationCode.mockReturnValue(of(undefined));

        component.changeInvitationCodeForm.controls.invitationCode.setErrors(['wrong']);
        component.changeInvitationCodeForm.controls.invitationCodeRepeat.patchValue('test-invitation-code');

        component.onSubmit();

        expect(httpServiceMock.postChangeInvitationCode).not.toHaveBeenCalled();
    });

    it('should not change the invitation-code (invitation-code-repeat wrong)', () => {

        httpServiceMock.postChangeInvitationCode.mockReturnValue(of(undefined));

        component.changeInvitationCodeForm.controls.invitationCode.patchValue('test-invitation-code');
        component.changeInvitationCodeForm.controls.invitationCodeRepeat.setErrors(['wrong']);

        component.onSubmit();

        expect(httpServiceMock.postChangeInvitationCode).not.toHaveBeenCalled();
    });

    it('should not change the invitation-code (invitation-code and invitation-code-repeat unequal)', () => {

        httpServiceMock.postChangeInvitationCode.mockReturnValue(of(undefined));

        component.changeInvitationCodeForm.controls.invitationCode.patchValue('test-invitation-code');
        component.changeInvitationCodeForm.controls.invitationCodeRepeat.patchValue('test-invitation-code-unequal');

        component.onSubmit();

        expect(httpServiceMock.postChangeInvitationCode).not.toHaveBeenCalled();
    });

    it('should not change the invitation-code (change invitation-code failed)', () => {

        httpServiceMock.postChangeInvitationCode.mockReturnValue(
            throwError(() => 'Error on change invitation-code')
        );

        component.changeInvitationCodeForm.controls.invitationCode.patchValue('test-invitation-code');
        component.changeInvitationCodeForm.controls.invitationCodeRepeat.patchValue('test-invitation-code');

        component.onSubmit();

        expect(httpServiceMock.postChangeInvitationCode).toHaveBeenCalledExactlyOnceWith('test-invitation-code');
    });
});

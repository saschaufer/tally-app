import {AsyncPipe} from "@angular/common";
import {Component} from "@angular/core";
import {ComponentFixture, TestBed} from '@angular/core/testing';
import {firstValueFrom} from "rxjs";
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {AuthService} from "../../services/auth.service";
import {ChangeInvitationCodeComponent} from "./change-invitation-code/change-invitation-code.component";
import {ChangeUserDetailsComponent} from "./change-user-details/change-user-details.component";
import {DeleteUserComponent} from "./delete-user/delete-user.component";
import {LoginDetailsComponent} from "./login-details/login-details.component";
import {SettingsComponent} from './settings.component';

@Component({selector: 'app-change-invitation-code', template: ''})
class ChangeInvitationCodeComponentMock implements Partial<ChangeInvitationCodeComponent> {
}

@Component({selector: 'app-change-user-details', template: ''})
class ChangeUserDetailsComponentMock implements Partial<ChangeUserDetailsComponent> {
}

@Component({selector: 'app-delete-user', template: ''})
class DeleteUserComponentMock implements Partial<DeleteUserComponent> {
}

@Component({selector: 'app-login-details', template: ''})
class LoginDetailsComponentMock implements Partial<LoginDetailsComponent> {
}

describe('SettingsComponent', () => {

    let component: SettingsComponent;
    let fixture: ComponentFixture<SettingsComponent>;

    const authServiceMock = vi.mockObject(AuthService.prototype);

    beforeEach(async () => {

        vi.resetAllMocks();

        await TestBed.configureTestingModule({
            providers: [
                {provide: AuthService, useValue: authServiceMock}
            ],
        }).overrideComponent(SettingsComponent, {
            set: {
                imports: [
                    LoginDetailsComponentMock,
                    ChangeUserDetailsComponentMock,
                    ChangeInvitationCodeComponentMock,
                    DeleteUserComponentMock,
                    AsyncPipe
                ]
            }
        }).compileComponents();

        fixture = TestBed.createComponent(SettingsComponent);
        component = fixture.componentInstance;
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    it('should tell that user is an admin', async () => {

        authServiceMock.isAdmin.mockReturnValue(true);
        fixture.detectChanges();

        expect(authServiceMock.isAdmin).toHaveBeenCalledTimes(1);
        expect(await firstValueFrom(component.isAdmin)).eq(true);
    });

    it('should tell that user is not an admin', async () => {

        authServiceMock.isAdmin.mockReturnValue(false);
        fixture.detectChanges();

        expect(authServiceMock.isAdmin).toHaveBeenCalledTimes(1);
        expect(await firstValueFrom(component.isAdmin)).eq(false);
    });
});

import {ComponentFixture, TestBed} from '@angular/core/testing';
import {MockComponent, MockProvider} from "ng-mocks";
import {AuthService} from "../../services/auth.service";
import {ChangeInvitationCodeComponent} from "./change-invitation-code/change-invitation-code.component";
import {ChangeUserDetailsComponent} from "./change-user-details/change-user-details.component";
import {DeleteUserComponent} from "./delete-user/delete-user.component";
import {LoginDetailsComponent} from "./login-details/login-details.component";

import {SettingsComponent} from './settings.component';
import SpyObj = jasmine.SpyObj;

describe('SettingsComponent', () => {

    let component: SettingsComponent;
    let fixture: ComponentFixture<SettingsComponent>;

    let authServiceSpy: SpyObj<AuthService>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [SettingsComponent],
            providers: [MockProvider(AuthService)],
            declarations: [
                MockComponent(LoginDetailsComponent),
                MockComponent(ChangeUserDetailsComponent),
                MockComponent(ChangeInvitationCodeComponent),
                MockComponent(DeleteUserComponent)
            ]
        })
            .compileComponents();

        fixture = TestBed.createComponent(SettingsComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();

        authServiceSpy = spyOnAllFunctions(TestBed.inject(AuthService));
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    it('should tell that user is an admin', () => {

        authServiceSpy.isAdmin.and.returnValue(true);

        component.ngOnInit();

        expect(authServiceSpy.isAdmin).toHaveBeenCalledTimes(1);
        expect(component.isAdmin()).toBeTruthy();
    });

    it('should tell that user is not an admin', () => {

        authServiceSpy.isAdmin.and.returnValue(false);

        component.ngOnInit();

        expect(authServiceSpy.isAdmin).toHaveBeenCalledTimes(1);

        expect(component.isAdmin()).toBeFalsy();
    });
});

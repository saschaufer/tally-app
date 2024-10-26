import {NgIf} from "@angular/common";
import {Component, inject} from '@angular/core';
import {AuthService} from "../../services/auth.service";
import {ChangeInvitationCodeComponent} from "./change-invitation-code/change-invitation-code.component";
import {ChangeUserDetailsComponent} from "./change-user-details/change-user-details.component";
import {DeleteUserComponent} from "./delete-user/delete-user.component";
import {LoginDetailsComponent} from "./login-details/login-details.component";

@Component({
    selector: 'app-settings',
    standalone: true,
    imports: [
        LoginDetailsComponent,
        ChangeUserDetailsComponent,
        ChangeInvitationCodeComponent,
        NgIf,
        DeleteUserComponent
    ],
    templateUrl: './settings.component.html',
    styles: ``
})
export class SettingsComponent {

    private authService = inject(AuthService);

    _isAdmin: boolean = false;

    ngOnInit(): void {
        this._isAdmin = this.authService.isAdmin();
    }

    isAdmin() {
        return this._isAdmin;
    }
}

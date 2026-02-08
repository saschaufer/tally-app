import {AsyncPipe} from "@angular/common";
import {Component, inject} from '@angular/core';
import {BehaviorSubject} from "rxjs";
import {AuthService} from "../../services/auth.service";
import {ChangeInvitationCodeComponent} from "./change-invitation-code/change-invitation-code.component";
import {ChangeUserDetailsComponent} from "./change-user-details/change-user-details.component";
import {DeleteUserComponent} from "./delete-user/delete-user.component";
import {LoginDetailsComponent} from "./login-details/login-details.component";

@Component({
    selector: 'app-settings',
    imports: [
        LoginDetailsComponent,
        ChangeUserDetailsComponent,
        ChangeInvitationCodeComponent,
        DeleteUserComponent,
        AsyncPipe
    ],
    templateUrl: './settings.component.html',
    styles: ``
})
export class SettingsComponent {

    private readonly authService = inject(AuthService);

    private readonly isAdminObserver = new BehaviorSubject<boolean>(false);
    isAdmin = this.isAdminObserver.asObservable();

    ngOnInit(): void {
        this.isAdminObserver.next(this.authService.isAdmin());
    }
}

import {Component} from '@angular/core';
import {ChangeUserDetailsComponent} from "./change-user-details/change-user-details.component";
import {LoginDetailsComponent} from "./login-details/login-details.component";

@Component({
    selector: 'app-settings',
    standalone: true,
    imports: [
        LoginDetailsComponent,
        ChangeUserDetailsComponent
    ],
    templateUrl: './settings.component.html',
    styles: ``
})
export class SettingsComponent {

}

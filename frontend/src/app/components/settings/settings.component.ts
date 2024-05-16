import {Component} from '@angular/core';
import {LoginDetailsComponent} from "./login-details/login-details.component";

@Component({
    selector: 'app-settings',
    standalone: true,
    imports: [
        LoginDetailsComponent
    ],
    templateUrl: './settings.component.html',
    styles: ``
})
export class SettingsComponent {

}

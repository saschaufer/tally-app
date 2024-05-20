import {NgClass, NgIf} from "@angular/common";
import {Component, inject} from '@angular/core';
import {Router, RouterLink, RouterLinkActive, RouterOutlet} from '@angular/router';
import {routeName} from "./app.routes";
import {AuthService} from "./services/auth.service";

@Component({
    selector: 'app-root',
    standalone: true,
    imports: [RouterOutlet, RouterLink, RouterLinkActive, NgClass, NgIf],
    templateUrl: './app.component.html',
    styles: [],
})
export class AppComponent {

    protected readonly routeName = routeName;

    private router = inject(Router);
    private authService = inject(AuthService);

    showNavBar(): boolean {
        return this.router.url != "/" + routeName.login
            && this.router.url != "/" + routeName.register;
    }

    isAdmin() {
        return this.authService.isAdmin();
    };
}

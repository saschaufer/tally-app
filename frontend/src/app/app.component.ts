import {NgClass, NgIf} from "@angular/common";
import {Component, inject} from '@angular/core';
import {Router, RouterLink, RouterLinkActive, RouterOutlet} from '@angular/router';
import {routeName} from "./app.routes";

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

    showNavBar(): boolean {
        console.log('route: ' + this.router.url);
        return this.router.url != "/" + routeName.login
            && this.router.url != "/" + routeName.register;
    }
}

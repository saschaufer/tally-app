import {NgClass, NgIf} from "@angular/common";
import {Component, inject} from '@angular/core';
import {Router, RouterLink, RouterLinkActive, RouterOutlet} from '@angular/router';
import {routeName} from "./app.routes";
import {AuthService} from "./services/auth.service";
import {HttpLoaderService} from "./services/http-loader.service";

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
    private loaderService = inject(HttpLoaderService);

    ngAfterViewInit() {
        this.loaderService.isLoading.subscribe((status: boolean) => {
            const dialog = document.getElementById('#app.loading')! as HTMLDialogElement;
            if (status) {
                dialog.showModal();
                document.body.classList.add('cursor-loader');
            } else {
                dialog.close();
                document.body.classList.remove('cursor-loader');
            }
        });
    }

    showNavBar(): boolean {
        return this.router.url != "/" + routeName.login
            && this.router.url != "/" + routeName.register
            && this.router.url != "/" + routeName.reset_password
            && !this.router.url.startsWith("/" + routeName.register_confirm);
    }

    isAdmin() {
        return this.authService.isAdmin();
    };
}

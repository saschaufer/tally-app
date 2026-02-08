import {AsyncPipe, NgClass} from "@angular/common";
import {Component, ElementRef, inject, ViewChild} from '@angular/core';
import {NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet} from '@angular/router';
import {BehaviorSubject} from "rxjs";
import {routeName} from "./app.routes";
import {AuthService} from "./services/auth.service";
import {HttpLoaderService} from "./services/http-loader.service";

@Component({
    selector: 'app-root',
    imports: [RouterOutlet, RouterLink, RouterLinkActive, NgClass, AsyncPipe],
    templateUrl: './app.component.html',
    styles: []
})
export class AppComponent {

    @ViewChild('app.loading', {static: true}) dialog!: ElementRef<HTMLDialogElement>;

    protected readonly routeName = routeName;

    private readonly router = inject(Router);
    private readonly authService = inject(AuthService);
    private readonly loaderService = inject(HttpLoaderService);

    private readonly showNavBarObserver = new BehaviorSubject<boolean>(false);
    private readonly isAdminObserver = new BehaviorSubject<boolean>(false);

    showNavBar = this.showNavBarObserver.asObservable();
    isAdmin = this.isAdminObserver.asObservable();

    ngAfterViewInit() {
        this.loaderService.isLoading.subscribe((status: boolean) => {
            const dialog: HTMLDialogElement = this.dialog.nativeElement;
            if (status) {
                dialog.showModal();
                document.body.classList.add('cursor-loader');
            } else {
                dialog.close();
                document.body.classList.remove('cursor-loader');
            }
        });

        this.router.events.subscribe((event) => {

            if (event instanceof NavigationEnd) {

                const show = this.router.url != "/" + routeName.login
                    && this.router.url != "/" + routeName.register
                    && this.router.url != "/" + routeName.reset_password
                    && !this.router.url.startsWith("/" + routeName.register_confirm);

                this.showNavBarObserver.next(show);
                this.isAdminObserver.next(this.authService.isAdmin());
            }
        });
    }
}

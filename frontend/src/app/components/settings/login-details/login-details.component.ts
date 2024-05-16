import {DatePipe} from "@angular/common";
import {Component, inject} from '@angular/core';
import {Router} from "@angular/router";
import {interval, Subscription} from "rxjs";
import {routeName} from "../../../app.routes";
import {AuthService} from "../../../services/auth.service";

@Component({
    selector: 'app-login-details',
    standalone: true,
    imports: [
        DatePipe
    ],
    templateUrl: './login-details.component.html',
    styles: ``
})
export class LoginDetailsComponent {

    private authService = inject(AuthService);
    private router = inject(Router);

    private time!: Subscription;

    jwtDetails = this.authService.getJwtDetails();

    ngOnInit() {
        this.time = interval(1000).subscribe(() => this.jwtDetails = this.authService.getJwtDetails());
    }

    ngOnDestroy() {
        this.time.unsubscribe();
    }

    onLogout() {
        this.authService.removeJwt();
        this.router.navigate(["/" + routeName.login]).then();
    }
}

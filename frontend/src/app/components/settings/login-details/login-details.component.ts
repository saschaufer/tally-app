import {DatePipe} from "@angular/common";
import {ChangeDetectorRef, Component, inject} from '@angular/core';
import {Router} from "@angular/router";
import {interval, Subscription} from "rxjs";
import {routeName} from "../../../app.routes";
import {AuthService} from "../../../services/auth.service";

@Component({
    selector: 'app-login-details',
    imports: [
        DatePipe
    ],
    templateUrl: './login-details.component.html',
    styles: ``
})
export class LoginDetailsComponent {

    private readonly authService = inject(AuthService);
    private readonly router = inject(Router);
    private readonly cdr = inject(ChangeDetectorRef);

    private time!: Subscription;

    jwtDetails = this.authService.getJwtDetails();

    ngOnInit() {
        this.time = interval(1000).subscribe(() => {
            this.jwtDetails = this.authService.getJwtDetails();
            this.cdr.detectChanges();
        });
    }

    ngOnDestroy() {
        this.time.unsubscribe();
    }

    onLogout() {
        this.authService.removeJwt();
        this.router.navigate(["/" + routeName.login]).then();
    }
}

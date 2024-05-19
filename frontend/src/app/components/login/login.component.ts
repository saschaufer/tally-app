import {Component, inject, NgZone} from '@angular/core';
import {FormControl, FormGroup, ReactiveFormsModule, Validators} from "@angular/forms";
import {Router, RouterLink} from "@angular/router";
import {routeName} from "../../app.routes";
import {AuthService} from "../../services/auth.service";
import {HttpService} from "../../services/http.service";


@Component({
    selector: 'app-login',
    standalone: true,
    imports: [
        ReactiveFormsModule,
        RouterLink
    ],
    templateUrl: './login.component.html',
    styles: ``
})
export class LoginComponent {

    protected readonly routeName = routeName;

    private authService = inject(AuthService);
    private httpService = inject(HttpService);
    private router = inject(Router);
    private zone = inject(NgZone);

    readonly loginForm = new FormGroup({
        username: new FormControl('', {nonNullable: true, validators: [Validators.required]}),
        password: new FormControl('', {nonNullable: true, validators: [Validators.required]})
    });

    onSubmit() {

        if (this.loginForm.valid) {

            const username = this.loginForm.controls.username.value;
            const password = this.loginForm.controls.password.value;

            this.loginForm.reset();

            this.httpService.postLogin(username, password)
                .subscribe({
                    next: (loginResponse) => {
                        if (this.authService.setJwt(loginResponse.jwt, loginResponse.secure)) {
                            console.log("Login successful");
                            this.zone.run(() =>
                                this.router.navigate(['/' + routeName.settings]).then()
                            ).then();
                        } else {
                            console.error("Cookie not set. Probably because it needs to be sent over a secure HTTPS connection.");
                        }
                    },
                    error: (error) => {
                        console.error(error);
                    }
                });
        }
    }
}

import {NgIf} from "@angular/common";
import {HttpErrorResponse} from "@angular/common/http";
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
        RouterLink,
        NgIf
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
        email: new FormControl('', {
            nonNullable: true,
            validators: [Validators.required, Validators.pattern('.+@.+\\..+')]
        }),
        password: new FormControl('', {nonNullable: true, validators: [Validators.required]})
    });

    emailInvalid: boolean = false;

    error: HttpErrorResponse | undefined;

    onSubmit() {

        this.emailInvalid = this.loginForm.get('email')!.hasError('pattern');

        if (this.loginForm.valid) {

            const email = this.loginForm.controls.email.value;
            const password = this.loginForm.controls.password.value;

            this.loginForm.reset();

            this.httpService.postLogin(email, password)
                .subscribe({
                    next: (loginResponse) => {
                        if (this.authService.setJwt(loginResponse.jwt, loginResponse.secure)) {
                            console.info("Login successful.");
                            this.zone.run(() =>
                                this.router.navigate(['/' + routeName.purchases_new]).then()
                            ).then();
                        } else {
                            console.error("Cookie not set. Probably because it needs to be sent over a secure HTTPS connection.");
                            this.openDialog('#login.errorLoginCookie');
                        }
                    },
                    error: (error) => {
                        console.error('Error on login.');
                        console.error(error);
                        this.error = error;
                        this.openDialog('#login.errorLogin');
                    }
                });
        }
    }

    openDialog(id: string) {
        const dialog = document.getElementById(id)! as HTMLDialogElement;
        dialog.addEventListener('click', () => {
            dialog.close();
        });
        dialog.showModal();
    }
}

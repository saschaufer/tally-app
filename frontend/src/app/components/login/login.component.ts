import {HttpErrorResponse} from "@angular/common/http";
import {Component, ElementRef, inject, ViewChild} from '@angular/core';
import {FormControl, FormGroup, ReactiveFormsModule, Validators} from "@angular/forms";
import {Router, RouterLink} from "@angular/router";
import {routeName} from "../../app.routes";
import {AuthService} from "../../services/auth.service";
import {HttpService} from "../../services/http.service";


@Component({
    selector: 'app-login',
    imports: [ReactiveFormsModule, RouterLink],
    templateUrl: './login.component.html',
    styles: ``
})
export class LoginComponent {

    @ViewChild('login.errorLogin', {static: true}) dialogError!: ElementRef<HTMLDialogElement>;
    @ViewChild('login.errorLoginCookie', {static: true}) dialogErrorCookie!: ElementRef<HTMLDialogElement>;

    protected readonly routeName = routeName;

    private readonly authService = inject(AuthService);
    private readonly httpService = inject(HttpService);
    private readonly router = inject(Router);

    readonly loginForm = new FormGroup({
        email: new FormControl('', {
            nonNullable: true,
            validators: [Validators.required, Validators.pattern('.+@.+\\..+')]
        }),
        password: new FormControl('', {nonNullable: true, validators: [Validators.required]})
    });

    formErrors = {
        emailMissing: false,
        emailInvalid: false,
        passwordMissing: false
    };

    error: HttpErrorResponse | undefined;

    onSubmit() {

        this.formErrors = this.getFormValidationErrors();

        if (this.loginForm.valid) {

            const email = this.loginForm.controls.email.value;
            const password = this.loginForm.controls.password.value;

            this.loginForm.reset();

            this.httpService.postLogin(email, password)
                .subscribe({
                    next: (loginResponse) => {
                        if (this.authService.setJwt(loginResponse.jwt, loginResponse.secure)) {
                            console.info("Login successful.");
                            this.router.navigate(['/' + routeName.purchases_new]).then();
                        } else {
                            console.error("Cookie not set. Probably because it needs to be sent over a secure HTTPS connection.");
                            this.openDialog(this.dialogErrorCookie.nativeElement);
                        }
                    },
                    error: (error) => {
                        console.error('Error on login.');
                        console.error(error);
                        this.error = error;
                        this.openDialog(this.dialogError.nativeElement);
                    }
                });
        }
    }

    openDialog(dialog: HTMLDialogElement) {
        dialog.addEventListener('click', () => {
            dialog.close();
        });
        dialog.showModal();
    }

    getFormValidationErrors() {

        let emailMissing = this.loginForm.get('email')!.hasError('required');
        let emailInvalid = this.loginForm.get('email')!.hasError('pattern');
        let passwordMissing = this.loginForm.get('password')!.hasError('required');

        return {
            emailMissing: emailMissing,
            emailInvalid: emailInvalid,
            passwordMissing: passwordMissing
        };
    }
}

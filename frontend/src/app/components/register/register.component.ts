import {HttpErrorResponse} from "@angular/common/http";
import {Component, inject, NgZone} from '@angular/core';
import {
    AbstractControl,
    FormControl,
    FormGroup,
    ReactiveFormsModule,
    ValidationErrors,
    ValidatorFn,
    Validators
} from "@angular/forms";
import {Router} from "@angular/router";
import {routeName} from "../../app.routes";
import {HttpService} from "../../services/http.service";

@Component({
    selector: 'app-register',
    standalone: true,
    imports: [
        ReactiveFormsModule
    ],
    templateUrl: './register.component.html',
    styles: ``
})
export class RegisterComponent {

    private httpService = inject(HttpService);
    private router = inject(Router);
    private zone = inject(NgZone);

    readonly passwordsMatch: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {

        const password = control.get('password')?.value;
        const passwordRepeat = control.get('passwordRepeat')?.value;

        // null => ok
        // passwordsMatch: true => there is an error on passwordsMatch
        return password === passwordRepeat ? null : {passwordsMatch: true};
    };

    readonly registerForm = new FormGroup({
        username: new FormControl('', {nonNullable: true, validators: [Validators.required]}),
        password: new FormControl('', {nonNullable: true, validators: [Validators.required]}),
        passwordRepeat: new FormControl('', {nonNullable: true, validators: [Validators.required]}),
        invitationCode: new FormControl('', {nonNullable: true, validators: [Validators.required]})
    }, {validators: this.passwordsMatch});

    onSubmit() {

        if (this.registerForm.valid) {

            const username = this.registerForm.controls.username.value;
            const password = this.registerForm.controls.password.value;
            const invitationCode = this.registerForm.controls.invitationCode.value;

            this.httpService.postRegisterNewUser(username, password, invitationCode)
                .subscribe({
                    next: () => {
                        console.log("Register successful");
                        this.zone.run(() =>
                            this.router.navigate(['/' + routeName.login]).then()
                        ).then();
                    },
                    error: (error: HttpErrorResponse) => {
                        console.error(error.status + ' ' + error.statusText + ': ' + error.error);
                    }
                });
        }
    }
}

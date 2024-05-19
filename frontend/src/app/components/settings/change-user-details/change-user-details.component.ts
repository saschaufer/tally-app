import {HttpErrorResponse} from "@angular/common/http";
import {Component, inject} from '@angular/core';
import {
    AbstractControl,
    FormControl,
    FormGroup,
    ReactiveFormsModule,
    ValidationErrors,
    ValidatorFn,
    Validators
} from "@angular/forms";
import {HttpService} from "../../../services/http.service";

@Component({
    selector: 'app-change-user-details',
    standalone: true,
    imports: [
        ReactiveFormsModule
    ],
    templateUrl: './change-user-details.component.html',
    styles: ``
})
export class ChangeUserDetailsComponent {

    private httpService = inject(HttpService);

    readonly passwordsMatch: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {

        const password = control.get('password')?.value;
        const passwordRepeat = control.get('passwordRepeat')?.value;

        // null => ok
        // passwordsMatch: true => there is an error on passwordsMatch
        return password === passwordRepeat ? null : {passwordsMatch: true};
    };

    readonly changePasswordForm = new FormGroup({
        password: new FormControl('', {nonNullable: true, validators: [Validators.required]}),
        passwordRepeat: new FormControl('', {nonNullable: true, validators: [Validators.required]}),
    }, {validators: this.passwordsMatch});

    onSubmit() {

        if (this.changePasswordForm.valid) {

            const password = this.changePasswordForm.controls.password.value;

            this.changePasswordForm.reset();

            this.httpService.postChangePassword(password)
                .subscribe({
                    next: () => {
                        console.log("Password change successful");
                    },
                    error: (error: HttpErrorResponse) => {
                        console.error(error.status + ' ' + error.statusText + ': ' + error.error);
                    }
                });
        }
    }
}

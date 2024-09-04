import {NgIf} from "@angular/common";
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
        ReactiveFormsModule,
        NgIf
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

    formErrors = {
        passwordMissing: false,
        passwordRepeatMissing: false,
        passwordUnequal: false
    };

    error: HttpErrorResponse | undefined;

    onSubmit() {

        this.formErrors = this.getFormValidationErrors();

        if (this.changePasswordForm.valid) {

            const password = this.changePasswordForm.controls.password.value;

            this.changePasswordForm.reset();

            this.httpService.postChangePassword(password)
                .subscribe({
                    next: () => {
                        console.info('Password change successful.');
                        this.openDialog('#settings.changeUserDetails.successChangePassword');
                    },
                    error: (error: HttpErrorResponse) => {
                        console.error('Error changing password.');
                        console.error(error);
                        this.error = error;
                        this.openDialog('#settings.changeUserDetails.errorChangePassword');
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

    getFormValidationErrors() {

        let passwordMissing = this.changePasswordForm.get('password')!.hasError('required');
        let passwordRepeatMissing = this.changePasswordForm.get('passwordRepeat')!.hasError('required');
        let passwordUnequal = false;

        if (!passwordMissing && !passwordRepeatMissing) {
            passwordUnequal = (this.changePasswordForm.errors !== null ? this.changePasswordForm.errors['passwordsMatch'] : false)
        }

        return {
            passwordMissing: passwordMissing,
            passwordRepeatMissing: passwordRepeatMissing,
            passwordUnequal: passwordUnequal
        };
    }
}

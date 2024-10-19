import {NgIf} from "@angular/common";
import {HttpErrorResponse} from "@angular/common/http";
import {Component, inject} from '@angular/core';
import {FormControl, FormGroup, ReactiveFormsModule, Validators} from "@angular/forms";
import {RouterLink} from "@angular/router";
import {routeName} from "../../app.routes";
import {HttpService} from "../../services/http.service";

@Component({
    selector: 'app-reset-password',
    standalone: true,
    imports: [
        NgIf,
        ReactiveFormsModule,
        RouterLink
    ],
    templateUrl: './reset-password.component.html',
    styles: ``
})
export class ResetPasswordComponent {

    private httpService = inject(HttpService);

    readonly resetPasswordForm = new FormGroup({
        email: new FormControl('', {
            nonNullable: true,
            validators: [Validators.required, Validators.pattern('.+@.+\\..+')]
        })
    });

    email = '';
    emailSent = false;
    emailInvalid: boolean = false;

    error: HttpErrorResponse | undefined;

    onSubmit() {

        this.emailInvalid = this.resetPasswordForm.get('email')!.hasError('pattern');

        if (this.resetPasswordForm.valid) {

            const email = this.resetPasswordForm.controls.email.value;

            this.resetPasswordForm.reset();

            this.httpService.postResetPassword(email)
                .subscribe({
                    next: () => {
                        console.info("Reset password successful.");
                        this.email = email;
                        this.emailSent = true;
                    },
                    error: (error) => {
                        console.error('Error on resetting password.');
                        console.error(error);
                        this.error = error;
                        this.openDialog('#reset-password.errorResetPassword');
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

    protected readonly routeName = routeName;
}

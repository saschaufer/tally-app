import {HttpErrorResponse} from "@angular/common/http";
import {ChangeDetectorRef, Component, ElementRef, inject, ViewChild} from '@angular/core';
import {
    AbstractControl,
    FormControl,
    FormGroup,
    ReactiveFormsModule,
    ValidationErrors,
    ValidatorFn,
    Validators
} from "@angular/forms";
import {RouterLink} from "@angular/router";
import {routeName} from "../../app.routes";
import {HttpService} from "../../services/http.service";

@Component({
    selector: 'app-register',
    imports: [
        ReactiveFormsModule,
        RouterLink
    ],
    templateUrl: './register.component.html',
    styles: ``
})
export class RegisterComponent {

    @ViewChild('register.errorRegister', {static: true}) dialog!: ElementRef<HTMLDialogElement>;

    protected readonly routeName = routeName;

    private readonly httpService = inject(HttpService);
    private readonly cdr = inject(ChangeDetectorRef);

    email = '';
    emailSent = false;

    readonly passwordsMatch: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {

        const password = control.get('password')?.value;
        const passwordRepeat = control.get('passwordRepeat')?.value;

        // null => ok
        // passwordsMatch: true => there is an error on passwordsMatch
        return password === passwordRepeat ? null : {passwordsMatch: true};
    };

    readonly registerForm = new FormGroup({
        email: new FormControl('', {
            nonNullable: true,
            validators: [Validators.required, Validators.pattern('.+@.+\\..+')]
        }),
        password: new FormControl('', {nonNullable: true, validators: [Validators.required]}),
        passwordRepeat: new FormControl('', {nonNullable: true, validators: [Validators.required]}),
        invitationCode: new FormControl('', {nonNullable: true, validators: [Validators.required]})
    }, {validators: this.passwordsMatch});

    formErrors = {
        emailMissing: false,
        emailInvalid: false,
        passwordMissing: false,
        passwordRepeatMissing: false,
        passwordUnequal: false,
        invitationCodeMissing: false
    };

    error: HttpErrorResponse | undefined;

    onSubmit() {

        this.formErrors = this.getFormValidationErrors();

        if (this.registerForm.valid) {

            const email = this.registerForm.controls.email.value;
            const password = this.registerForm.controls.password.value;
            const invitationCode = this.registerForm.controls.invitationCode.value;

            this.registerForm.reset();

            this.httpService.postRegisterNewUser(email, password, invitationCode)
                .subscribe({
                    next: () => {
                        console.info('Register successful.');
                        this.email = email;
                        this.emailSent = true;
                        this.cdr.detectChanges();
                    },
                    error: (error: HttpErrorResponse) => {
                        console.error('Error register.');
                        console.error(error);
                        this.error = error;
                        this.openDialog(this.dialog.nativeElement);
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

        let emailMissing = this.registerForm.get('email')!.hasError('required');
        let emailInvalid = this.registerForm.get('email')!.hasError('pattern');
        let passwordMissing = this.registerForm.get('password')!.hasError('required');
        let passwordRepeatMissing = this.registerForm.get('passwordRepeat')!.hasError('required');
        let invitationCodeMissing = this.registerForm.get('invitationCode')!.hasError('required');
        let passwordUnequal = false;

        if (!passwordMissing && !passwordRepeatMissing) {
            passwordUnequal = (this.registerForm.errors !== null ? this.registerForm.errors['passwordsMatch'] : false)
        }

        return {
            emailMissing: emailMissing,
            emailInvalid: emailInvalid,
            passwordMissing: passwordMissing,
            passwordRepeatMissing: passwordRepeatMissing,
            invitationCodeMissing: invitationCodeMissing,
            passwordUnequal: passwordUnequal
        };
    }
}

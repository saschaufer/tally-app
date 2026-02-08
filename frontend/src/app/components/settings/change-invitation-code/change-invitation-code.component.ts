import {HttpErrorResponse} from "@angular/common/http";
import {Component, ElementRef, inject, ViewChild} from '@angular/core';
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
    selector: 'app-change-invitation-code',
    imports: [ReactiveFormsModule],
    templateUrl: './change-invitation-code.component.html',
    styles: ``
})
export class ChangeInvitationCodeComponent {

    @ViewChild('settings.changeInvitationCode.successChangeInvitationCode', {static: true}) dialogSuccessChangeInvitationCode!: ElementRef<HTMLDialogElement>;
    @ViewChild('settings.changeInvitationCode.errorChangeInvitationCode', {static: true}) dialogErrorChangeInvitationCode!: ElementRef<HTMLDialogElement>;

    private readonly httpService = inject(HttpService);

    readonly invitationCodesMatch: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {

        const invitationCode = control.get('invitationCode')?.value;
        const invitationCodeRepeat = control.get('invitationCodeRepeat')?.value;

        // null => ok
        // passwordsMatch: true => there is an error on passwordsMatch
        return invitationCode === invitationCodeRepeat ? null : {invitationCodesMatch: true};
    };

    readonly changeInvitationCodeForm = new FormGroup({
        invitationCode: new FormControl('', {nonNullable: true, validators: [Validators.required]}),
        invitationCodeRepeat: new FormControl('', {nonNullable: true, validators: [Validators.required]}),
    }, {validators: this.invitationCodesMatch});

    formErrors = {
        invitationCodeMissing: false,
        invitationCodeRepeatMissing: false,
        invitationCodeUnequal: false
    };

    error: HttpErrorResponse | undefined;

    onSubmit() {

        this.formErrors = this.getFormValidationErrors();

        if (this.changeInvitationCodeForm.valid) {

            const invitationCode = this.changeInvitationCodeForm.controls.invitationCode.value;

            this.changeInvitationCodeForm.reset();

            this.httpService.postChangeInvitationCode(invitationCode)
                .subscribe({
                    next: () => {
                        console.info('Invitation code change successful.');
                        this.openDialog(this.dialogSuccessChangeInvitationCode.nativeElement);
                    },
                    error: (error: HttpErrorResponse) => {
                        console.error('Error changing invitation code.');
                        console.error(error);
                        this.error = error;
                        this.openDialog(this.dialogErrorChangeInvitationCode.nativeElement);
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

        let invitationCodeMissing = this.changeInvitationCodeForm.get('invitationCode')!.hasError('required');
        let invitationCodeRepeatMissing = this.changeInvitationCodeForm.get('invitationCodeRepeat')!.hasError('required');
        let invitationCodeUnequal = false;

        if (!invitationCodeMissing && !invitationCodeRepeatMissing) {
            invitationCodeUnequal = (this.changeInvitationCodeForm.errors !== null ? this.changeInvitationCodeForm.errors['invitationCodesMatch'] : false)
        }

        return {
            invitationCodeMissing: invitationCodeMissing,
            invitationCodeRepeatMissing: invitationCodeRepeatMissing,
            invitationCodeUnequal: invitationCodeUnequal
        };
    }
}

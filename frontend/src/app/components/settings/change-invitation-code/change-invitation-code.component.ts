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
    selector: 'app-change-invitation-code',
    standalone: true,
    imports: [
        ReactiveFormsModule
    ],
    templateUrl: './change-invitation-code.component.html',
    styles: ``
})
export class ChangeInvitationCodeComponent {

    private httpService = inject(HttpService);

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

    onSubmit() {

        if (this.changeInvitationCodeForm.valid) {

            const invitationCode = this.changeInvitationCodeForm.controls.invitationCode.value;

            this.changeInvitationCodeForm.reset();

            this.httpService.postChangeInvitationCode(invitationCode)
                .subscribe({
                    next: () => {
                        console.log("Invitation code change successful");
                    },
                    error: (error: HttpErrorResponse) => {
                        console.error(error.status + ' ' + error.statusText + ': ' + error.error);
                    }
                });
        }
    }
}

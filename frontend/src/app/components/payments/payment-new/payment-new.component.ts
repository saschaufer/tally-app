import {HttpErrorResponse} from "@angular/common/http";
import {Component, ElementRef, inject, ViewChild} from '@angular/core';
import {FormControl, FormGroup, ReactiveFormsModule, Validators} from "@angular/forms";
import {RouterLink} from "@angular/router";
import {Big} from "big.js";
import {routeName} from "../../../app.routes";
import {HttpService} from "../../../services/http.service";

@Component({
    selector: 'app-payment-new',
    imports: [ReactiveFormsModule, RouterLink],
    templateUrl: './payment-new.component.html',
    styles: ``
})
export class PaymentNewComponent {

    @ViewChild('payments.paymentNew.successCreatePayment', {static: true}) dialogSuccess!: ElementRef<HTMLDialogElement>;
    @ViewChild('payments.paymentNew.errorCreatePayment', {static: true}) dialogError!: ElementRef<HTMLDialogElement>;

    protected readonly routeName = routeName;

    private readonly httpService = inject(HttpService);

    readonly newPaymentForm = new FormGroup({
        amount: new FormControl('', {
            nonNullable: true,
            validators: [Validators.required, Validators.pattern("^\\d{1,10}(\\.\\d{1,10})?$")]
        })
    });

    formErrors = {
        amountMissing: false,
        amountPatternInvalid: false
    };

    error: HttpErrorResponse | undefined;

    onSubmit() {

        this.formErrors = this.getFormValidationErrors();

        if (this.newPaymentForm.valid) {

            const amount = Big(this.newPaymentForm.controls.amount.value);

            this.newPaymentForm.reset();

            this.httpService.postCreatePayment(amount)
                .subscribe({
                    next: () => {
                        console.info('Payment created.');
                        this.openDialog(this.dialogSuccess.nativeElement);
                    },
                    error: (error) => {
                        console.error('Error creating payment.');
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
        return {
            amountMissing: this.newPaymentForm.get('amount')!.hasError('required'),
            amountPatternInvalid: this.newPaymentForm.get('amount')!.hasError('pattern')
        }
    }
}

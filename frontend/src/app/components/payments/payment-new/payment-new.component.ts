import {Component, inject} from '@angular/core';
import {FormControl, FormGroup, ReactiveFormsModule, Validators} from "@angular/forms";
import {RouterLink} from "@angular/router";
import {Big} from "big.js";
import {routeName} from "../../../app.routes";
import {HttpService} from "../../../services/http.service";

@Component({
    selector: 'app-payment-new',
    standalone: true,
    imports: [
        ReactiveFormsModule,
        RouterLink
    ],
    templateUrl: './payment-new.component.html',
    styles: ``
})
export class PaymentNewComponent {

    protected readonly routeName = routeName;

    private httpService = inject(HttpService);

    readonly newPaymentForm = new FormGroup({
        amount: new FormControl('', {nonNullable: true, validators: [Validators.required]})
    });

    onSubmit() {

        if (this.newPaymentForm.valid) {

            const amount = Big(this.newPaymentForm.controls.amount.value);

            this.newPaymentForm.reset();

            this.httpService.postCreatePayment(amount)
                .subscribe({
                    next: () => {
                        console.log("Payment created");
                    },
                    error: (error) => {
                        console.error(error.status + ' ' + error.statusText + ': ' + error.error);
                    }
                });
        }
    }
}

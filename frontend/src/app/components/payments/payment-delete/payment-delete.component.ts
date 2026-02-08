import {DatePipe} from "@angular/common";
import {HttpErrorResponse} from "@angular/common/http";
import {Component, ElementRef, inject, ViewChild} from '@angular/core';
import {ActivatedRoute, Router, RouterLink} from "@angular/router";
import {Big} from "big.js";
import {routeName} from "../../../app.routes";
import {HttpService} from "../../../services/http.service";
import {GetPaymentsResponse} from "../../../services/models/GetPaymentsResponse";

@Component({
    selector: 'app-payment-delete',
    imports: [
        DatePipe,
        RouterLink
    ],
    templateUrl: './payment-delete.component.html',
    styles: ``
})
export class PaymentDeleteComponent {

    @ViewChild('payments.paymentDelete.successDeletePayment', {static: true}) dialogSuccess!: ElementRef<HTMLDialogElement>;
    @ViewChild('settings.paymentDelete.errorDeletePayment', {static: true}) dialogError!: ElementRef<HTMLDialogElement>;

    protected readonly routeName = routeName;

    private readonly activatedRoute = inject(ActivatedRoute);
    private readonly httpService = inject(HttpService);
    private readonly router = inject(Router);

    payment: GetPaymentsResponse | undefined;

    error: HttpErrorResponse | undefined;

    ngOnInit(): void {
        this.activatedRoute.params.subscribe(params => {
            const urlAppend = params['payment'];
            const json = window.atob(decodeURIComponent(urlAppend));
            let payment = JSON.parse(json) as GetPaymentsResponse;
            this.payment = {
                id: payment.id,
                amount: Big(payment.amount),  // JSON.parse makes a string, therefore, need to be set to Big explicitly
                timestamp: payment.timestamp
            }
        });
    }

    onClickDelete() {
        this.httpService.postDeletePayment(this.payment!.id)
            .subscribe({
                next: () => {
                    console.info("Payment delete.");
                    this.openDialogSuccess();
                },
                error: (error: HttpErrorResponse) => {
                    console.error('Error deleting payment.');
                    console.error(error);
                    this.error = error;
                    this.openDialogError();
                }
            });
    }

    openDialogSuccess() {
        const dialog: HTMLDialogElement = this.dialogSuccess.nativeElement;
        dialog.addEventListener('click', () => {
            dialog.close();
            this.router.navigate(['/' + routeName.payments]).then();
        });
        dialog.showModal();
    }

    openDialogError() {
        const dialog: HTMLDialogElement = this.dialogError.nativeElement;
        dialog.addEventListener('click', () => {
            dialog.close();
        });
        dialog.showModal();
    }
}

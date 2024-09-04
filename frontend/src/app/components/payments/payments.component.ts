import {DatePipe, NgClass, NgForOf, NgIf} from "@angular/common";
import {HttpErrorResponse} from "@angular/common/http";
import {Component, inject, NgZone} from '@angular/core';
import {Router, RouterLink} from "@angular/router";
import {Big} from "big.js";
import {routeName} from "../../app.routes";
import {HttpService} from "../../services/http.service";
import {GetAccountBalanceResponse} from "../../services/models/GetAccountBalanceResponse";
import {GetPaymentsResponse} from "../../services/models/GetPaymentsResponse";

@Component({
    selector: 'app-payments',
    standalone: true,
    imports: [
        NgForOf,
        NgIf,
        RouterLink,
        DatePipe,
        NgClass
    ],
    templateUrl: './payments.component.html',
    styles: ``
})
export class PaymentsComponent {

    protected readonly routeName = routeName;

    private httpService = inject(HttpService);
    private router = inject(Router);
    private zone = inject(NgZone);

    payments: GetPaymentsResponse[] | undefined;
    accountBalance: GetAccountBalanceResponse | undefined;
    isNegative = false;

    error: HttpErrorResponse | undefined;

    ngOnInit(): void {

        this.httpService.getReadPayments()
            .subscribe({
                next: payments => {
                    console.info("Payments read.");
                    this.payments = payments;
                },
                error: (error: HttpErrorResponse) => {
                    console.error('Error reading payments.');
                    console.error(error);
                    this.error = error;
                    this.openDialog('#payments.errorReadingPayments');
                }
            });

        this.httpService.getReadAccountBalance()
            .subscribe({
                next: accountBalance => {
                    console.info("Account balance read.");
                    this.accountBalance = accountBalance;
                    this.isNegative = Big(this.accountBalance.amountTotal).lt(Big('0.0'));
                },
                error: (error: HttpErrorResponse) => {
                    console.error('Error reading account balance.');
                    console.error(error);
                    this.error = error;
                    this.openDialog('#payments.errorReadingAccountBalance');
                }
            });
    }

    onClick(i: number) {
        let payment = this.payments![i];
        const urlAppend = encodeURIComponent(window.btoa(JSON.stringify(payment)));
        this.zone.run(() =>
            this.router.navigate(['/' + routeName.payments_delete + '/' + urlAppend]).then()
        ).then();
    }

    openDialog(id: string) {
        const dialog = document.getElementById(id)! as HTMLDialogElement;
        dialog.addEventListener('click', () => {
            dialog.close();
        });
        dialog.showModal();
    }
}

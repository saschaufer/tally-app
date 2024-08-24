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

    ngOnInit(): void {

        this.httpService.getReadPayments()
            .subscribe({
                next: payments => {
                    console.log("Payments read");
                    this.payments = payments;
                },
                error: (error: HttpErrorResponse) => {
                    console.error(error.status + ' ' + error.statusText + ': ' + error.error);
                }
            });

        this.httpService.getReadAccountBalance()
            .subscribe({
                next: accountBalance => {
                    console.log("Account balance read");
                    this.accountBalance = accountBalance;
                    this.isNegative = Big(this.accountBalance.amountTotal).lt(Big('0.0'));
                },
                error: (error: HttpErrorResponse) => {
                    console.error(error.status + ' ' + error.statusText + ': ' + error.error);
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
}

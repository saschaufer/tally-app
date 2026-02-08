import {DatePipe, NgClass} from "@angular/common";
import {HttpErrorResponse} from "@angular/common/http";
import {ChangeDetectorRef, Component, ElementRef, inject, ViewChild} from '@angular/core';
import {Router, RouterLink} from "@angular/router";
import {Big} from "big.js";
import {routeName} from "../../app.routes";
import {HttpService} from "../../services/http.service";
import {GetAccountBalanceResponse} from "../../services/models/GetAccountBalanceResponse";
import {GetPaymentsResponse} from "../../services/models/GetPaymentsResponse";

@Component({
    selector: 'app-payments',
    imports: [
        RouterLink,
        DatePipe,
        NgClass
    ],
    templateUrl: './payments.component.html',
    styles: ``
})
export class PaymentsComponent {

    @ViewChild('payments.errorReadingPayments', {static: true}) dialogErrorReadingPayments!: ElementRef<HTMLDialogElement>;
    @ViewChild('payments.errorReadingAccountBalance', {static: true}) dialogErrorReadingAccountBalance!: ElementRef<HTMLDialogElement>;

    protected readonly routeName = routeName;

    private readonly httpService = inject(HttpService);
    private readonly router = inject(Router);
    private readonly cdr = inject(ChangeDetectorRef);

    payments: GetPaymentsResponse[] = [];
    accountBalance: GetAccountBalanceResponse | undefined;
    isNegative = false;

    error: HttpErrorResponse | undefined;

    ngOnInit(): void {

        this.httpService.getReadPayments()
            .subscribe({
                next: payments => {
                    console.info("Payments read: " + payments.length + " payments found.");
                    payments.sort((a, b) => new Date(a.timestamp) < new Date(b.timestamp) ? 1 : -1);
                    this.payments = payments;
                    this.cdr.detectChanges();
                },
                error: (error: HttpErrorResponse) => {
                    console.error('Error reading payments.');
                    console.error(error);
                    this.error = error;
                    this.openDialog(this.dialogErrorReadingPayments.nativeElement);
                }
            });

        this.httpService.getReadAccountBalance()
            .subscribe({
                next: accountBalance => {
                    console.info("Account balance read.");
                    this.accountBalance = accountBalance;
                    this.isNegative = Big(this.accountBalance.amountTotal).lt(Big('0.0'));
                    this.cdr.detectChanges();
                },
                error: (error: HttpErrorResponse) => {
                    console.error('Error reading account balance.');
                    console.error(error);
                    this.error = error;
                    this.openDialog(this.dialogErrorReadingAccountBalance.nativeElement);
                }
            });
    }

    onClick(i: number) {
        const payment = this.payments[i];
        const urlAppend = encodeURIComponent(window.btoa(JSON.stringify(payment)));
        this.router.navigate(['/' + routeName.payments_delete + '/' + urlAppend]).then();
    }

    openDialog(dialog: HTMLDialogElement) {
        dialog.addEventListener('click', () => {
            dialog.close();
        });
        dialog.showModal();
    }
}

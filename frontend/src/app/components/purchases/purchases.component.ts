import {NgClass, NgForOf, NgIf} from "@angular/common";
import {HttpErrorResponse} from "@angular/common/http";
import {Component, inject, NgZone} from '@angular/core';
import {Router, RouterLink} from "@angular/router";
import {Big} from "big.js";
import {routeName} from "../../app.routes";
import {HttpService} from "../../services/http.service";
import {GetAccountBalanceResponse} from "../../services/models/GetAccountBalanceResponse";
import {GetPurchasesResponse} from "../../services/models/GetPurchasesResponse";

@Component({
    selector: 'app-purchases',
    standalone: true,
    imports: [
        NgForOf,
        NgIf,
        RouterLink,
        NgClass
    ],
    templateUrl: './purchases.component.html',
    styles: ``
})
export class PurchasesComponent {

    protected readonly routeName = routeName;

    private httpService = inject(HttpService);
    private router = inject(Router);
    private zone = inject(NgZone);

    purchases: GetPurchasesResponse[] | undefined;
    accountBalance: GetAccountBalanceResponse | undefined;
    isNegative = false;

    error: HttpErrorResponse | undefined;

    ngOnInit(): void {

        this.httpService.getReadPurchases()
            .subscribe({
                next: purchases => {
                    console.info("Purchases read.");
                    this.purchases = purchases;
                },
                error: (error: HttpErrorResponse) => {
                    console.error('Error reading purchases.');
                    console.error(error);
                    this.error = error;
                    this.openDialog('#purchases.errorReadingPurchases');
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
                    this.openDialog('#purchases.errorReadingAccountBalance');
                }
            });
    }

    onClick(i: number) {
        let purchase = this.purchases![i];
        const urlAppend = encodeURIComponent(window.btoa(JSON.stringify(purchase)));
        this.zone.run(() =>
            this.router.navigate(['/' + routeName.purchases_delete + '/' + urlAppend]).then()
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

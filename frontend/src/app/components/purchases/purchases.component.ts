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

    ngOnInit(): void {

        this.httpService.getReadPurchases()
            .subscribe({
                next: purchases => {
                    console.log("Purchases read");
                    this.purchases = purchases;
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
        let purchase = this.purchases![i];
        const urlAppend = encodeURIComponent(window.btoa(JSON.stringify(purchase)));
        this.zone.run(() =>
            this.router.navigate(['/' + routeName.purchases_delete + '/' + urlAppend]).then()
        ).then();
    }
}

import {DatePipe, NgClass} from "@angular/common";
import {HttpErrorResponse} from "@angular/common/http";
import {ChangeDetectorRef, Component, ElementRef, inject, ViewChild} from '@angular/core';
import {Router, RouterLink} from "@angular/router";
import {Big} from "big.js";
import {routeName} from "../../app.routes";
import {HttpService} from "../../services/http.service";
import {GetAccountBalanceResponse} from "../../services/models/GetAccountBalanceResponse";
import {GetPurchasesResponse} from "../../services/models/GetPurchasesResponse";
import {PropertiesService} from "../../services/properties.service";

@Component({
    selector: 'app-purchases',
    imports: [
        RouterLink,
        NgClass,
        DatePipe
    ],
    templateUrl: './purchases.component.html',
    styles: ``
})
export class PurchasesComponent {

    @ViewChild('purchases.errorReadingPurchases', {static: true}) dialogErrorReadingPurchases!: ElementRef<HTMLDialogElement>;
    @ViewChild('purchases.errorReadingAccountBalance', {static: true}) dialogErrorReadingAccountBalance!: ElementRef<HTMLDialogElement>;

    protected readonly routeName = routeName;

    private readonly httpService = inject(HttpService);
    private readonly router = inject(Router);
    private readonly cdr = inject(ChangeDetectorRef);
    private readonly propertiesService = inject(PropertiesService);

    purchases: GetPurchasesResponse[] = [];
    accountBalance: GetAccountBalanceResponse | undefined;
    isNegative = false;
    currency = this.propertiesService.getProperties().currency;

    error: HttpErrorResponse | undefined;

    ngOnInit(): void {

        this.httpService.getReadPurchases()
            .subscribe({
                next: purchases => {
                    console.info("Purchases read: " + purchases.length + " purchases founds.");
                    purchases.sort((a, b) => new Date(a.purchaseTimestamp) < new Date(b.purchaseTimestamp) ? 1 : -1);
                    this.purchases = purchases;
                    this.cdr.detectChanges();
                },
                error: (error: HttpErrorResponse) => {
                    console.error('Error reading purchases.');
                    console.error(error);
                    this.error = error;
                    this.openDialog(this.dialogErrorReadingPurchases.nativeElement);
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
        const purchase = this.purchases[i];
        const urlAppend = encodeURIComponent(window.btoa(JSON.stringify(purchase)));
        this.router.navigate(['/' + routeName.purchases_delete + '/' + urlAppend]).then();
    }

    openDialog(dialog: HTMLDialogElement) {
        dialog.addEventListener('click', () => {
            dialog.close();
        });
        dialog.showModal();
    }
}

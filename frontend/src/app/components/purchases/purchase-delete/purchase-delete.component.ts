import {DatePipe} from "@angular/common";
import {HttpErrorResponse} from "@angular/common/http";
import {Component, ElementRef, inject, ViewChild} from '@angular/core';
import {ActivatedRoute, Router, RouterLink} from "@angular/router";
import {Big} from "big.js";
import {routeName} from "../../../app.routes";
import {HttpService} from "../../../services/http.service";
import {GetPurchasesResponse} from "../../../services/models/GetPurchasesResponse";
import {PropertiesService} from "../../../services/properties.service";

@Component({
    selector: 'app-purchase-delete',
    imports: [
        RouterLink,
        DatePipe
    ],
    templateUrl: './purchase-delete.component.html',
    styles: ``
})
export class PurchaseDeleteComponent {

    @ViewChild('purchases.purchaseDelete.successDeletingPurchase', {static: true}) dialogSuccess!: ElementRef<HTMLDialogElement>;
    @ViewChild('purchases.purchaseDelete.errorDeletingPurchase', {static: true}) dialogError!: ElementRef<HTMLDialogElement>;

    protected readonly routeName = routeName;

    private readonly activatedRoute = inject(ActivatedRoute);
    private readonly httpService = inject(HttpService);
    private readonly router = inject(Router);
    private readonly propertiesService = inject(PropertiesService);

    purchase: GetPurchasesResponse | undefined;
    currency = this.propertiesService.getProperties().currency;

    error: HttpErrorResponse | undefined;

    ngOnInit(): void {
        this.activatedRoute.params.subscribe(params => {
            const urlAppend = params['purchase'];
            const json = window.atob(decodeURIComponent(urlAppend));
            let purchase = JSON.parse(json) as GetPurchasesResponse;
            this.purchase = {
                purchaseId: purchase.purchaseId,
                productName: purchase.productName,
                productPrice: Big(purchase.productPrice),  // JSON.parse makes a string, therefore, need to be set to Big explicitly
                purchaseTimestamp: purchase.purchaseTimestamp
            }
        });
    }

    onClickDelete() {
        this.httpService.postDeletePurchase(this.purchase!.purchaseId)
            .subscribe({
                next: () => {
                    console.info("Purchase delete.");
                    this.openDialogSuccess();
                },
                error: (error: HttpErrorResponse) => {
                    console.error('Error deleting purchase.');
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
            this.router.navigate(['/' + routeName.purchases]).then();
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

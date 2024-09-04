import {DatePipe} from "@angular/common";
import {HttpErrorResponse} from "@angular/common/http";
import {Component, inject, NgZone} from '@angular/core';
import {ActivatedRoute, Router, RouterLink} from "@angular/router";
import {Big} from "big.js";
import {routeName} from "../../../app.routes";
import {HttpService} from "../../../services/http.service";
import {GetPurchasesResponse} from "../../../services/models/GetPurchasesResponse";

@Component({
    selector: 'app-purchase-delete',
    standalone: true,
    imports: [
        RouterLink,
        DatePipe
    ],
    templateUrl: './purchase-delete.component.html',
    styles: ``
})
export class PurchaseDeleteComponent {

    protected readonly routeName = routeName;

    private activatedRoute = inject(ActivatedRoute);
    private httpService = inject(HttpService);
    private router = inject(Router);
    private zone = inject(NgZone);

    purchase: GetPurchasesResponse | undefined;

    error: HttpErrorResponse | undefined;

    ngOnInit(): void {
        this.activatedRoute.params.subscribe(params => {
            const urlAppend = params['purchase'];
            const json = window.atob(decodeURIComponent(urlAppend));
            let purchase = JSON.parse(json) as GetPurchasesResponse;
            this.purchase = {
                purchaseId: purchase.purchaseId,
                productName: purchase.productName,
                productPrice: Big(purchase.productPrice),  // JSON.parse makes a string, therefor need to be set to Big explicitly
                purchaseTimestamp: purchase.purchaseTimestamp
            }
        });
    }

    onClickDelete() {
        this.httpService.postDeletePurchase(this.purchase!.purchaseId)
            .subscribe({
                next: () => {
                    console.info("Purchase delete.");
                    const dialog = document.getElementById('#purchases.purchaseDelete.successDeletingPurchase')! as HTMLDialogElement;
                    dialog.addEventListener('click', () => {
                        dialog.close();
                        this.zone.run(() =>
                            this.router.navigate(['/' + routeName.purchases]).then()
                        ).then();
                    });
                    dialog.showModal();
                },
                error: (error: HttpErrorResponse) => {
                    console.error('Error deleting purchase.');
                    console.error(error);
                    this.error = error;
                    this.openDialog('#purchases.purchaseDelete.errorDeletingPurchase');
                }
            });
    }

    openDialog(id: string) {
        const dialog = document.getElementById(id)! as HTMLDialogElement;
        dialog.addEventListener('click', () => {
            dialog.close();
        });
        dialog.showModal();
    }
}

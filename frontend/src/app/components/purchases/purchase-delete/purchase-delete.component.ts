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

    ngOnInit(): void {
        this.activatedRoute.params.subscribe(params => {
            const base64 = params['purchase'];
            const json = window.atob(base64);
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
                    console.log("Purchase delete");
                    this.zone.run(() =>
                        this.router.navigate(['/' + routeName.purchases]).then()
                    ).then();
                },
                error: (error: HttpErrorResponse) => {
                    console.error(error.status + ' ' + error.statusText + ': ' + error.error);
                }
            });
    }
}

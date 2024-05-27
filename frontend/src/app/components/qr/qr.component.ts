import {DatePipe} from "@angular/common";
import {HttpErrorResponse} from "@angular/common/http";
import {Component, inject, NgZone} from '@angular/core';
import {ActivatedRoute, Router, RouterLink} from "@angular/router";
import {routeName} from "../../app.routes";
import {HttpService} from "../../services/http.service";
import {GetProductsResponse} from "../../services/models/GetProductsResponse";

@Component({
    selector: 'app-qr',
    standalone: true,
    imports: [
        DatePipe,
        RouterLink
    ],
    templateUrl: './qr.component.html',
    styles: ``
})
export class QrComponent {

    protected readonly routeName = routeName;

    private activatedRoute = inject(ActivatedRoute);
    private httpService = inject(HttpService);
    private router = inject(Router);
    private zone = inject(NgZone);

    product: GetProductsResponse | undefined;

    ngOnInit(): void {

        let productId;

        this.activatedRoute.params.subscribe(params => {
            productId = Number.parseInt(params['productId'], 10);
        });

        if (productId) {

            this.httpService.postReadProduct(productId)
                .subscribe({
                    next: (product) => {
                        console.log("Product read");
                        this.product = product;
                    },
                    error: (error: HttpErrorResponse) => {
                        console.error(error.status + ' ' + error.statusText + ': ' + error.error);
                    }
                });
        }
    }

    onClickPurchase() {
        if (this.product) {
            this.httpService.postCreatePurchase(this.product.id)
                .subscribe({
                    next: () => {
                        console.log("Purchase created");
                        this.zone.run(() =>
                            this.router.navigate(['/' + routeName.purchases]).then()
                        ).then();
                    },
                    error: (error) => {
                        console.error(error.status + ' ' + error.statusText + ': ' + error.error);
                    }
                });

            this.product = undefined;
        }
    }
}

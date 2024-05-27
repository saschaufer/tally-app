import {NgForOf, NgIf} from "@angular/common";
import {HttpErrorResponse} from "@angular/common/http";
import {Component, inject, NgZone} from '@angular/core';
import {FormsModule, ReactiveFormsModule} from "@angular/forms";
import {Router, RouterLink} from "@angular/router";
import {routeName} from "../../../app.routes";
import {HttpService} from "../../../services/http.service";
import {GetProductsResponse} from "../../../services/models/GetProductsResponse";

@Component({
    selector: 'app-purchase-new',
    standalone: true,
    imports: [
        FormsModule,
        ReactiveFormsModule,
        RouterLink,
        NgForOf,
        NgIf
    ],
    templateUrl: './purchase-new.component.html',
    styles: ``
})
export class PurchaseNewComponent {

    protected readonly routeName = routeName;

    private httpService = inject(HttpService);
    private router = inject(Router);
    private zone = inject(NgZone);

    products: GetProductsResponse[] | undefined;
    selectedProduct: GetProductsResponse | undefined;

    ngOnInit(): void {

        this.httpService.getReadProducts()
            .subscribe({
                next: products => {
                    console.log("Products read");
                    this.products = products;
                },
                error: (error: HttpErrorResponse) => {
                    console.error(error.status + ' ' + error.statusText + ': ' + error.error);
                }
            });
    }

    onClick(i: number) {
        this.selectedProduct = this.products![i];
    }

    onSubmit() {
        if (this.selectedProduct) {
            this.httpService.postCreatePurchase(this.selectedProduct.id)
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

            this.selectedProduct = undefined;
        }
    }
}

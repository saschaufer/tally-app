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

    error: HttpErrorResponse | undefined;

    ngOnInit(): void {

        this.httpService.getReadProducts()
            .subscribe({
                next: products => {
                    console.info("Products read.");
                    this.products = products;
                },
                error: (error: HttpErrorResponse) => {
                    console.error('Error reading products.');
                    console.error(error);
                    this.error = error;
                    this.openDialog('#purchases.purchaseNew.errorReadingProducts');
                }
            });
    }

    onClick(i: number) {
        this.selectedProduct = this.products![i];
    }

    onSubmit() {
        
        if (!this.selectedProduct) {
            console.error('No product selected.');
            this.openDialog('#purchases.purchaseNew.errorNoProductSelected');
            return;
        }

        this.httpService.postCreatePurchase(this.selectedProduct.id)
            .subscribe({
                next: () => {
                    console.info("Purchase created.");
                    const dialog = document.getElementById('#purchases.purchaseNew.successCreatingPurchase')! as HTMLDialogElement;
                    dialog.addEventListener('click', () => {
                        dialog.close();
                        this.zone.run(() =>
                            this.router.navigate(['/' + routeName.purchases]).then()
                        ).then();
                    });
                    dialog.showModal();
                },
                error: (error) => {
                    console.error('Error creating purchase.');
                    console.error(error);
                    this.error = error;
                    this.openDialog('#purchases.purchaseNew.errorCreatingPurchase');
                }
            });

        this.selectedProduct = undefined;
    }

    openDialog(id: string) {
        const dialog = document.getElementById(id)! as HTMLDialogElement;
        dialog.addEventListener('click', () => {
            dialog.close();
        });
        dialog.showModal();
    }
}

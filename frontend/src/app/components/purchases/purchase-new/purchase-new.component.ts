import {HttpErrorResponse} from "@angular/common/http";
import {ChangeDetectorRef, Component, ElementRef, inject, ViewChild} from '@angular/core';
import {FormsModule, ReactiveFormsModule} from "@angular/forms";
import {Router, RouterLink} from "@angular/router";
import {routeName} from "../../../app.routes";
import {HttpService} from "../../../services/http.service";
import {GetProductsResponse} from "../../../services/models/GetProductsResponse";

@Component({
    selector: 'app-purchase-new',
    imports: [
        FormsModule,
        ReactiveFormsModule,
        RouterLink
    ],
    templateUrl: './purchase-new.component.html',
    styles: ``
})
export class PurchaseNewComponent {

    @ViewChild('purchases.purchaseNew.errorReadingProducts', {static: true}) dialogErrorReadingProducts!: ElementRef<HTMLDialogElement>;
    @ViewChild('purchases.purchaseNew.errorNoProductSelected', {static: true}) dialogErrorNoProductSelected!: ElementRef<HTMLDialogElement>;
    @ViewChild('purchases.purchaseNew.successCreatingPurchase', {static: true}) dialogSuccessCreatingPurchase!: ElementRef<HTMLDialogElement>;
    @ViewChild('purchases.purchaseNew.errorCreatingPurchase', {static: true}) dialogErrorCreatingPurchase!: ElementRef<HTMLDialogElement>;

    protected readonly routeName = routeName;

    private readonly httpService = inject(HttpService);
    private readonly router = inject(Router);
    private readonly cdr = inject(ChangeDetectorRef);

    products: GetProductsResponse[] = [];
    selectedProduct: GetProductsResponse | undefined;

    error: HttpErrorResponse | undefined;

    ngOnInit(): void {

        this.httpService.getReadProducts()
            .subscribe({
                next: products => {
                    console.info("Products read: " + products.length + " products found.");
                    products.sort((a, b) => a.name.localeCompare(b.name));
                    this.products = products;
                    this.cdr.detectChanges();
                },
                error: (error: HttpErrorResponse) => {
                    console.error('Error reading products.');
                    console.error(error);
                    this.error = error;
                    this.openDialog(this.dialogErrorReadingProducts.nativeElement);
                }
            });
    }

    onClick(i: number) {
        this.selectedProduct = this.products[i];
    }

    onSubmit() {

        if (!this.selectedProduct) {
            console.error('No product selected.');
            this.openDialog(this.dialogErrorNoProductSelected.nativeElement);
            return;
        }

        this.httpService.postCreatePurchase(this.selectedProduct.id)
            .subscribe({
                next: () => {
                    console.info("Purchase created.");
                    this.openDialogPurchaseCreated();
                },
                error: (error) => {
                    console.error('Error creating purchase.');
                    console.error(error);
                    this.error = error;
                    this.openDialog(this.dialogErrorCreatingPurchase.nativeElement);
                }
            });

        this.selectedProduct = undefined;
    }

    openDialogPurchaseCreated() {
        const dialog: HTMLDialogElement = this.dialogSuccessCreatingPurchase.nativeElement;
        dialog.addEventListener('click', () => {
            dialog.close();
            this.router.navigate(['/' + routeName.purchases]).then();
        });
        dialog.showModal();
    }

    openDialog(dialog: HTMLDialogElement) {
        dialog.addEventListener('click', () => {
            dialog.close();
        });
        dialog.showModal();
    }
}

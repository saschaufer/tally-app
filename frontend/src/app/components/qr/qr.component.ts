import {HttpErrorResponse} from "@angular/common/http";
import {ChangeDetectorRef, Component, ElementRef, inject, ViewChild} from '@angular/core';
import {ActivatedRoute, Router} from "@angular/router";
import {routeName} from "../../app.routes";
import {HttpService} from "../../services/http.service";
import {GetProductsResponse} from "../../services/models/GetProductsResponse";

@Component({
    selector: 'app-qr',
    imports: [],
    templateUrl: './qr.component.html',
    styles: ``
})
export class QrComponent {

    @ViewChild('qr.successCreatingPurchase', {static: true}) dialogSuccessCreatingPurchase!: ElementRef<HTMLDialogElement>;
    @ViewChild('qr.errorReadingProduct', {static: true}) dialogErrorReadingProduct!: ElementRef<HTMLDialogElement>;
    @ViewChild('qr.errorCreatingPurchase', {static: true}) dialogErrorCreatingPurchase!: ElementRef<HTMLDialogElement>;

    protected readonly routeName = routeName;

    private readonly activatedRoute = inject(ActivatedRoute);
    private readonly httpService = inject(HttpService);
    private readonly router = inject(Router);
    private readonly cdr = inject(ChangeDetectorRef);

    product: GetProductsResponse | undefined;

    error: HttpErrorResponse | undefined;

    ngOnInit(): void {

        let productId;

        this.activatedRoute.params.subscribe(params => {
            productId = Number.parseInt(params['productId'], 10);
        });

        if (productId) {

            this.httpService.postReadProduct(productId)
                .subscribe({
                    next: (product) => {
                        console.info("Product read.");
                        this.product = product;
                        this.cdr.detectChanges();
                    },
                    error: (error: HttpErrorResponse) => {
                        console.error('Error reading product.');
                        console.error(error);
                        this.error = error;
                        this.openDialog(this.dialogErrorReadingProduct.nativeElement);
                    }
                });
        }
    }

    onClickPurchase() {
        if (this.product) {
            this.httpService.postCreatePurchase(this.product.id)
                .subscribe({
                    next: () => {
                        console.info("Purchase created.");
                        this.openDialogSuccess(this.dialogSuccessCreatingPurchase.nativeElement);
                    },
                    error: (error) => {
                        console.error('Error creating purchase.');
                        console.error(error);
                        this.error = error;
                        this.openDialog(this.dialogErrorCreatingPurchase.nativeElement);
                    }
                });

            this.product = undefined;
        }
    }

    openDialogSuccess(dialog: HTMLDialogElement) {
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

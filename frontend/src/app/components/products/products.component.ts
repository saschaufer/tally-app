import {HttpErrorResponse} from "@angular/common/http";
import {ChangeDetectorRef, Component, ElementRef, inject, ViewChild} from '@angular/core';
import {Router, RouterLink} from "@angular/router";
import {routeName} from "../../app.routes";
import {HttpService} from "../../services/http.service";
import {GetProductsResponse} from "../../services/models/GetProductsResponse";
import {PropertiesService} from "../../services/properties.service";

@Component({
    selector: 'app-products',
    imports: [RouterLink],
    templateUrl: './products.component.html',
    styles: ``
})
export class ProductsComponent {

    @ViewChild('products.errorReadingProducts', {static: true}) dialogError!: ElementRef<HTMLDialogElement>;

    protected readonly routeName = routeName;

    private readonly httpService = inject(HttpService);
    private readonly router = inject(Router);
    private readonly cdr = inject(ChangeDetectorRef);
    private readonly propertiesService = inject(PropertiesService);

    products: GetProductsResponse[] = [];
    currency = this.propertiesService.getProperties().currency;

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
                    this.openDialog(this.dialogError.nativeElement);
                }
            });
    }

    onClick(i: number) {
        const product = this.products[i];
        const urlAppend = encodeURIComponent(window.btoa(JSON.stringify(product)));
        this.router.navigate(['/' + routeName.products_edit + '/' + urlAppend]).then();
    }

    openDialog(dialog: HTMLDialogElement) {
        dialog.addEventListener('click', () => {
            dialog.close();
        });
        dialog.showModal();
    }
}

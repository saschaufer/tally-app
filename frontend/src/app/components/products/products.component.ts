import {NgForOf, NgIf} from "@angular/common";
import {HttpErrorResponse} from "@angular/common/http";
import {Component, inject, NgZone} from '@angular/core';
import {Router, RouterLink} from "@angular/router";
import {routeName} from "../../app.routes";
import {HttpService} from "../../services/http.service";
import {GetProductsResponse} from "../../services/models/GetProductsResponse";

@Component({
    selector: 'app-products',
    standalone: true,
    imports: [
        NgForOf,
        NgIf,
        RouterLink
    ],
    templateUrl: './products.component.html',
    styles: ``
})
export class ProductsComponent {

    protected readonly routeName = routeName;

    private httpService = inject(HttpService);
    private router = inject(Router);
    private zone = inject(NgZone);

    products: GetProductsResponse[] | undefined;

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
                    this.openDialog('#products.errorReadingProducts');
                }
            });
    }

    onClick(i: number) {
        let product = this.products![i];
        const urlAppend = encodeURIComponent(window.btoa(JSON.stringify(product)));
        this.zone.run(() =>
            this.router.navigate(['/' + routeName.products_edit + '/' + urlAppend]).then()
        ).then();
    }

    openDialog(id: string) {
        const dialog = document.getElementById(id)! as HTMLDialogElement;
        dialog.addEventListener('click', () => {
            dialog.close();
        });
        dialog.showModal();
    }
}

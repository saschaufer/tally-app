import {Component, inject} from '@angular/core';
import {FormControl, FormGroup, ReactiveFormsModule, Validators} from "@angular/forms";
import {ActivatedRoute, RouterLink} from "@angular/router";
import {Big} from "big.js";
import {routeName} from "../../../app.routes";
import {HttpService} from "../../../services/http.service";
import {GetProductsResponse} from "../../../services/models/GetProductsResponse";

@Component({
    selector: 'app-product-edit',
    standalone: true,
    imports: [
        RouterLink,
        ReactiveFormsModule
    ],
    templateUrl: './product-edit.component.html',
    styles: ``
})
export class ProductEditComponent {

    protected readonly routeName = routeName;

    private httpService = inject(HttpService);
    private activatedRoute = inject(ActivatedRoute);

    product: GetProductsResponse | undefined;

    readonly changeNameForm = new FormGroup({
        name: new FormControl('', {nonNullable: true, validators: [Validators.required]}),
    });

    readonly changePriceForm = new FormGroup({
        price: new FormControl('', {nonNullable: true, validators: [Validators.required]}),
    });

    ngOnInit(): void {
        this.activatedRoute.params.subscribe(params => {
            const base64 = params['product'];
            const json = window.atob(base64);
            let product = JSON.parse(json) as GetProductsResponse;
            this.product = {
                id: product.id,
                name: product.name,
                price: Big(product.price) // JSON.parse makes a string, therefor need to be set to Big explicitly
            };
            this.changeNameForm.controls.name.patchValue(this.product.name);
            this.changePriceForm.controls.price.patchValue(this.product.price.toString());
        });
    }

    onSubmitChangeName() {

        if (this.changeNameForm.valid) {

            const name = this.changeNameForm.controls.name.value;

            this.changeNameForm.reset();

            this.httpService.postUpdateProduct(this.product!.id, name)
                .subscribe({
                    next: () => {
                        console.log("Product name changed");
                    },
                    error: (error) => {
                        console.error(error.status + ' ' + error.statusText + ': ' + error.error);
                    }
                });
        }
    }

    onSubmitChangePrice() {

        if (this.changePriceForm.valid) {

            const price = Big(this.changePriceForm.controls.price.value);

            this.changePriceForm.reset();

            this.httpService.postUpdateProductPrice(this.product!.id, price)
                .subscribe({
                    next: () => {
                        console.log("Product price changed");
                    },
                    error: (error) => {
                        console.error(error.status + ' ' + error.statusText + ': ' + error.error);
                    }
                });
        }
    }
}

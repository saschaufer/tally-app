import {Component, inject} from '@angular/core';
import {FormControl, FormGroup, ReactiveFormsModule, Validators} from "@angular/forms";
import {RouterLink} from "@angular/router";
import {Big} from "big.js";
import {routeName} from "../../../app.routes";
import {HttpService} from "../../../services/http.service";

@Component({
    selector: 'app-product-new',
    standalone: true,
    imports: [
        ReactiveFormsModule,
        RouterLink
    ],
    templateUrl: './product-new.component.html',
    styles: ``
})
export class ProductNewComponent {

    protected readonly routeName = routeName;

    private httpService = inject(HttpService);

    readonly newProductForm = new FormGroup({
        name: new FormControl('', {nonNullable: true, validators: [Validators.required]}),
        price: new FormControl('', {nonNullable: true, validators: [Validators.required]})
    });

    onSubmit() {

        if (this.newProductForm.valid) {

            const name = this.newProductForm.controls.name.value;
            const price = Big(this.newProductForm.controls.price.value);

            this.newProductForm.reset();

            this.httpService.postCreateProduct(name, price)
                .subscribe({
                    next: () => {
                        console.log("Product created");
                    },
                    error: (error) => {
                        console.error(error.status + ' ' + error.statusText + ': ' + error.error);
                    }
                });
        }
    }
}

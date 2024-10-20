import {NgIf} from "@angular/common";
import {HttpErrorResponse} from "@angular/common/http";
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
        ReactiveFormsModule,
        NgIf
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
        price: new FormControl('', {
            nonNullable: true,
            validators: [Validators.required, Validators.pattern("^\\d{1,10}(\\.\\d{1,10})?$")]
        }),
    });

    formErrorsChangeName = {
        nameMissing: false
    };

    formErrorsChangePrice = {
        priceMissing: false,
        pricePatternInvalid: false
    };

    error: HttpErrorResponse | undefined;

    ngOnInit(): void {
        this.activatedRoute.params.subscribe(params => {
            const urlAppend = params['product'];
            const json = window.atob(decodeURIComponent(urlAppend));
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

        this.formErrorsChangeName = this.getFormValidationErrorsChangeName();

        if (this.changeNameForm.valid) {

            const name = this.changeNameForm.controls.name.value;

            this.changeNameForm.reset();

            this.httpService.postUpdateProduct(this.product!.id, name)
                .subscribe({
                    next: () => {
                        console.info("Product name changed.");
                        this.openDialog('#products.productEdit.successChangeName');
                    },
                    error: (error) => {
                        console.error('Error changing product name.');
                        console.error(error);
                        this.error = error;
                        this.openDialog('#products.productEdit.errorChangeName');
                    }
                });
        }
    }

    onSubmitChangePrice() {

        this.formErrorsChangePrice = this.getFormValidationErrorsChangePrice();

        if (this.changePriceForm.valid) {

            const price = Big(this.changePriceForm.controls.price.value);

            this.changePriceForm.reset();

            this.httpService.postUpdateProductPrice(this.product!.id, price)
                .subscribe({
                    next: () => {
                        console.info("Product price changed.");
                        this.openDialog('#products.productEdit.successChangePrice');
                    },
                    error: (error) => {
                        console.error('Error changing product price.');
                        console.error(error);
                        this.error = error;
                        this.openDialog('#products.productEdit.errorChangePrice');
                    }
                });
        }
    }

    onClickDelete() {
        this.httpService.postDeleteProduct(this.product!.id)
            .subscribe({
                next: () => {
                    console.info("Product deleted.");
                    this.openDialog('#products.productEdit.successDelete');
                },
                error: (error) => {
                    console.error('Error deleting product.');
                    console.error(error);
                    this.error = error;
                    this.openDialog('#products.productEdit.errorDelete');
                }
            });
    }

    openDialog(id: string) {
        const dialog = document.getElementById(id)! as HTMLDialogElement;
        dialog.addEventListener('click', () => {
            dialog.close();
        });
        dialog.showModal();
    }

    getFormValidationErrorsChangeName() {
        return {
            nameMissing: this.changeNameForm.get('name')!.hasError('required')
        };
    }

    getFormValidationErrorsChangePrice() {
        return {
            priceMissing: this.changePriceForm.get('price')!.hasError('required'),
            pricePatternInvalid: this.changePriceForm.get('price')!.hasError('pattern')
        };
    }
}

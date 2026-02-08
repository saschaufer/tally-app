import {HttpErrorResponse} from "@angular/common/http";
import {Component, ElementRef, inject, ViewChild} from '@angular/core';
import {FormControl, FormGroup, ReactiveFormsModule, Validators} from "@angular/forms";
import {RouterLink} from "@angular/router";
import {Big} from "big.js";
import {routeName} from "../../../app.routes";
import {HttpService} from "../../../services/http.service";

@Component({
    selector: 'app-product-new',
    imports: [
        ReactiveFormsModule,
        RouterLink
    ],
    templateUrl: './product-new.component.html',
    styles: ``
})
export class ProductNewComponent {

    @ViewChild('products.productNew.successCreateProduct', {static: true}) dialogSuccessCreateProduct!: ElementRef<HTMLDialogElement>;
    @ViewChild('products.productNew.errorCreateProduct', {static: true}) dialogErrorCreateProduct!: ElementRef<HTMLDialogElement>;

    protected readonly routeName = routeName;

    private readonly httpService = inject(HttpService);

    readonly newProductForm = new FormGroup({
        name: new FormControl('', {nonNullable: true, validators: [Validators.required]}),
        price: new FormControl('', {
            nonNullable: true,
            validators: [Validators.required, Validators.pattern("^\\d{1,10}(\\.\\d{1,10})?$")]
        })
    });

    formErrors = {
        nameMissing: false,
        priceMissing: false,
        pricePatternInvalid: false
    };

    error: HttpErrorResponse | undefined;

    onSubmit() {

        this.formErrors = this.getFormValidationErrors();

        if (this.newProductForm.valid) {

            const name = this.newProductForm.controls.name.value;
            const price = Big(this.newProductForm.controls.price.value);

            this.newProductForm.reset();

            this.httpService.postCreateProduct(name, price)
                .subscribe({
                    next: () => {
                        console.info("Product created.");
                        this.openDialog(this.dialogSuccessCreateProduct.nativeElement);
                    },
                    error: (error) => {
                        console.error('Error creating product.');
                        console.error(error);
                        this.error = error;
                        this.openDialog(this.dialogErrorCreateProduct.nativeElement);
                    }
                });
        }
    }

    openDialog(dialog: HTMLDialogElement) {
        dialog.addEventListener('click', () => {
            dialog.close();
        });
        dialog.showModal();
    }

    getFormValidationErrors() {
        return {
            nameMissing: this.newProductForm.get('name')!.hasError('required'),
            priceMissing: this.newProductForm.get('price')!.hasError('required'),
            pricePatternInvalid: this.newProductForm.get('price')!.hasError('pattern')
        }
    }
}

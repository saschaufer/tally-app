import {DatePipe} from "@angular/common";
import {HttpErrorResponse} from "@angular/common/http";
import {Component, inject, NgZone} from '@angular/core';
import {ActivatedRoute, Router, RouterLink} from "@angular/router";
import {Big} from "big.js";
import {routeName} from "../../../app.routes";
import {HttpService} from "../../../services/http.service";
import {GetPaymentsResponse} from "../../../services/models/GetPaymentsResponse";

@Component({
    selector: 'app-payment-delete',
    standalone: true,
    imports: [
        DatePipe,
        RouterLink
    ],
    templateUrl: './payment-delete.component.html',
    styles: ``
})
export class PaymentDeleteComponent {

    protected readonly routeName = routeName;

    private activatedRoute = inject(ActivatedRoute);
    private httpService = inject(HttpService);
    private router = inject(Router);
    private zone = inject(NgZone);

    payment: GetPaymentsResponse | undefined;

    ngOnInit(): void {
        this.activatedRoute.params.subscribe(params => {
            const urlAppend = params['payment'];
            const json = window.atob(decodeURIComponent(urlAppend));
            let payment = JSON.parse(json) as GetPaymentsResponse;
            this.payment = {
                id: payment.id,
                amount: Big(payment.amount),  // JSON.parse makes a string, therefor need to be set to Big explicitly
                timestamp: payment.timestamp
            }
        });
    }

    onClickDelete() {
        this.httpService.postDeletePayment(this.payment!.id)
            .subscribe({
                next: () => {
                    console.log("Payment delete");
                    this.zone.run(() =>
                        this.router.navigate(['/' + routeName.payments]).then()
                    ).then();
                },
                error: (error: HttpErrorResponse) => {
                    console.error(error.status + ' ' + error.statusText + ': ' + error.error);
                }
            });
    }
}

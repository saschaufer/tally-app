import {HttpClient} from "@angular/common/http";
import {inject, Injectable} from '@angular/core';
import {Observable} from "rxjs";
import {AuthService} from "./auth.service";
import {GetAccountBalanceResponse} from "./models/GetAccountBalanceResponse";
import {GetPaymentsResponse} from "./models/GetPaymentsResponse";
import {GetProductsResponse} from "./models/GetProductsResponse";
import {GetPurchasesResponse} from "./models/GetPurchasesResponse";
import {GetUsersResponse} from "./models/GetUsersResponse";
import {LoginResponse} from "./models/LoginResponse";

@Injectable({
    providedIn: 'root'
})
export class HttpService {

    private httpClient = inject(HttpClient);
    private authService = inject(AuthService);

    postLogin(email: string, password: string): Observable<LoginResponse> {

        const httpOptions = {
            headers: {
                // Prevent Browsers from pop up a login window if response is 401
                'X-Requested-With': 'XMLHttpRequest',
                Authorization: 'Basic ' + window.btoa(email + ':' + password),
                'X-UserId': email
            },
            responseType: 'json' as const
        };

        return this.httpClient.post<LoginResponse>("/login", null, httpOptions);
    }

    postRegisterNewUser(email: string, password: string, invitationCode: string): Observable<void> {

        const httpOptions = {
            headers: {
                // Prevent Browsers from pop up a login window if response is 401
                'X-Requested-With': 'XMLHttpRequest',
                Authorization: 'Basic ' + window.btoa('invitation-code:' + invitationCode),
                'X-UserId': email
            }
        };

        const body = {
            email: email,
            password: password
        };

        return this.httpClient.post<void>("/register", body, httpOptions);
    }

    postRegisterNewUserConfirm(email: string, secret: string): Observable<void> {

        const httpOptions = {
            headers: {
                'X-UserId': email
            }
        };

        const body = {
            email: email,
            registrationSecret: secret
        };

        return this.httpClient.post<void>("/register/confirm", body, httpOptions);
    }

    postResetPassword(email: string): Observable<void> {

        const httpOptions = {
            headers: {
                'X-UserId': email
            }
        };

        return this.httpClient.post<void>("/reset-password", email, httpOptions);
    }

    postChangePassword(password: string): Observable<void> {
        return this.httpClient.post<void>("/settings/change-password", password, this.httpOptions());
    }

    postChangeInvitationCode(invitationCode: string): Observable<void> {
        return this.httpClient.post<void>("/settings/change-invitation-code", invitationCode, this.httpOptions());
    }

    getReadUsers(): Observable<GetUsersResponse[]> {
        return this.httpClient.get<GetUsersResponse[]>("/users", this.httpOptions());
    }

    getReadProducts(): Observable<GetProductsResponse[]> {
        return this.httpClient.get<GetProductsResponse[]>("/products", this.httpOptions());
    }

    postReadProduct(id: number): Observable<GetProductsResponse> {

        const body = {
            id: id
        };

        return this.httpClient.post<GetProductsResponse>("/products/read-product", body, this.httpOptions());
    }

    postCreateProduct(name: string, price: Big): Observable<void> {

        const body = {
            name: name,
            price: price
        };

        return this.httpClient.post<void>("/products/create-product", body, this.httpOptions());
    }

    postUpdateProduct(id: number, name: string): Observable<void> {

        const body = {
            id: id,
            name: name
        };

        return this.httpClient.post<void>("/products/update-product", body, this.httpOptions());
    }

    postDeleteProduct(id: number): Observable<void> {
        return this.httpClient.post<void>("/products/delete-product", id, this.httpOptions());
    }

    postUpdateProductPrice(id: number, price: Big): Observable<void> {

        const body = {
            id: id,
            price: price
        };

        return this.httpClient.post<void>("/products/update-price", body, this.httpOptions());
    }

    getReadPurchases(): Observable<GetPurchasesResponse[]> {
        return this.httpClient.get<GetPurchasesResponse[]>("/purchases", this.httpOptions());
    }

    postCreatePurchase(id: number): Observable<void> {

        const body = {
            productId: id
        };

        return this.httpClient.post<void>("/purchases/create-purchase", body, this.httpOptions());
    }

    postDeletePurchase(id: number): Observable<void> {

        const body = {
            purchaseId: id
        };

        return this.httpClient.post<void>("/purchases/delete-purchase", body, this.httpOptions());
    }

    getReadPayments(): Observable<GetPaymentsResponse[]> {
        return this.httpClient.get<GetPaymentsResponse[]>("/payments", this.httpOptions());
    }

    postCreatePayment(amount: Big): Observable<void> {

        const body = {
            amount: amount
        };

        return this.httpClient.post<void>("/payments/create-payment", body, this.httpOptions());
    }

    postDeletePayment(paymentId: number): Observable<void> {

        const body = {
            paymentId: paymentId
        };

        return this.httpClient.post<void>("/payments/delete-payment", body, this.httpOptions());
    }

    getReadAccountBalance(): Observable<GetAccountBalanceResponse> {
        return this.httpClient.get<GetAccountBalanceResponse>("/account-balance", this.httpOptions());
    }

    private httpOptions() {
        const jwt = this.authService.getJwt();

        return {
            headers: {
                Authorization: 'Bearer ' + jwt,
                'X-UserId': this.authService.getJwtDetails().email
            }
        };
    }
}

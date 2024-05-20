import {HttpClient} from "@angular/common/http";
import {inject, Injectable} from '@angular/core';
import {Observable} from "rxjs";
import {AuthService} from "./auth.service";
import {GetProductsResponse} from "./models/GetProductsResponse";
import {LoginResponse} from "./models/LoginResponse";

@Injectable({
    providedIn: 'root'
})
export class HttpService {

    private httpClient = inject(HttpClient);
    private authService = inject(AuthService);

    postLogin(username: string, password: string): Observable<LoginResponse> {

        const httpOptions = {
            headers: {
                // Prevent Browsers from pop up a login window if response is 401
                'X-Requested-With': 'XMLHttpRequest',
                Authorization: 'Basic ' + window.btoa(username + ':' + password)
            },
            responseType: 'json' as const
        };

        return this.httpClient.post<LoginResponse>("/login", null, httpOptions);
    }

    postRegisterNewUser(username: string, password: string, invitationCode: string): Observable<void> {

        const httpOptions = {
            headers: {
                // Prevent Browsers from pop up a login window if response is 401
                'X-Requested-With': 'XMLHttpRequest',
                Authorization: 'Basic ' + window.btoa('invitation-code:' + invitationCode)
            }
        };

        const body = {
            username: username,
            password: password
        };

        return this.httpClient.post<void>("/register", body, httpOptions);
    }

    postChangePassword(password: string): Observable<void> {
        return this.httpClient.post<void>("/settings/change-password", password, this.httpOptions());
    }

    postChangeInvitationCode(invitationCode: string): Observable<void> {
        return this.httpClient.post<void>("/settings/change-invitation-code", invitationCode, this.httpOptions());
    }

    getReadProducts(): Observable<GetProductsResponse[]> {
        return this.httpClient.get<GetProductsResponse[]>("/products", this.httpOptions());
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

    postUpdateProductPrice(id: number, price: Big): Observable<void> {

        const body = {
            id: id,
            price: price
        };

        return this.httpClient.post<void>("/products/update-price", body, this.httpOptions());
    }

    private httpOptions() {
        const jwt = this.authService.getJwt();

        return {
            headers: {
                Authorization: 'Bearer ' + jwt
            }
        };
    }
}

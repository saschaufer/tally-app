import {HttpClient} from "@angular/common/http";
import {inject, Injectable} from '@angular/core';
import {Observable} from "rxjs";
import {LoginResponse} from "./models/LoginResponse";

@Injectable({
    providedIn: 'root'
})
export class HttpService {

    private httpClient = inject(HttpClient);

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
}

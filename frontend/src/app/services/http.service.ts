import {HttpClient} from "@angular/common/http";
import {inject, Injectable} from '@angular/core';
import {Observable} from "rxjs";
import {AuthService} from "./auth.service";
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

        const jwt = this.authService.getJwt();

        const httpOptions = {
            headers: {
                Authorization: 'Bearer ' + jwt
            }
        };

        return this.httpClient.post<void>("/settings/change-password", password, httpOptions);
    }

    postChangeInvitationCode(invitationCode: string): Observable<void> {

        const jwt = this.authService.getJwt();

        const httpOptions = {
            headers: {
                Authorization: 'Bearer ' + jwt
            }
        };

        return this.httpClient.post<void>("/settings/change-invitation-code", invitationCode, httpOptions);
    }
}

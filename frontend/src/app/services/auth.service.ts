import {inject, Injectable} from '@angular/core';
import {CookieService} from "ngx-cookie-service";
import {Jwt} from "./models/Jwt";
import {JwtDetails} from "./models/JwtDetails";

export enum role {
    admin = 'admin',
    user = 'user'
}

@Injectable({
    providedIn: 'root'
})
export class AuthService {

    private cookieService = inject(CookieService);

    private readonly cookieName: string = 'TALLY_JWT';

    isAuthenticated() {
        return this.cookieService.check(this.cookieName);
    }

    isAdmin() {
        return this.hasRoles([role.admin]);
    }

    hasRoles(expectedRoles: readonly role[]) {

        const jwt = this.getJwt();

        if (!jwt) {
            return false;
        }

        const decodedJwt = this.decodeJwt(jwt);

        for (let r of expectedRoles) {
            if (!decodedJwt.authorities.includes(r)) {
                return false;
            }
        }

        return true;
    }

    setJwt(jwt: string, secure: boolean) {

        const decodedJwt = this.decodeJwt(jwt);

        console.debug('Set Cookie for JWT.');

        this.cookieService.set(this.cookieName, jwt, {
            sameSite: "Strict",
            secure: secure,
            expires: new Date(decodedJwt.exp * 1000),
        });

        console.debug('Cookie for JWT set.', new Map().set('check', this.getJwt()));

        return this.isAuthenticated();
    }

    getJwt() {
        return this.cookieService.get(this.cookieName);
    }

    removeJwt() {
        this.cookieService.delete(this.cookieName);
    }

    public getJwtDetails(): JwtDetails {

        const jwt = this.cookieService.get(this.cookieName);
        const decodedJwt = this.decodeJwt(jwt);

        const expire = new Date(decodedJwt.exp * 1000);
        const now = new Date();

        return {
            email: decodedJwt.sub,
            issuedAt: decodedJwt.iat * 1000,
            expiresAt: decodedJwt.exp * 1000,
            expiresLeft: new Date(0, 0, 0, 0, 0, 0, 0).setUTCMilliseconds(expire.getTime() - now.getTime()),
            authorities: decodedJwt.authorities
        };
    }

    /**
     * Decodes the given JWTs payload (the second part behind the first dot).
     * @param token the JWT
     * @return the decoded payload part as {@link Jwt}
     * @private
     */
    private decodeJwt(token: string): Jwt {
        const base64Url = token.split('.')[1];
        const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
        const jsonPayload = decodeURIComponent(window.atob(base64).split('').map(function (c) {
            return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
        }).join(''));

        return JSON.parse(jsonPayload) as Jwt;
    }
}

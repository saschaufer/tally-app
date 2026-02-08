import {TestBed} from '@angular/core/testing';
import {beforeEach, describe, expect, it, Mock, vi} from 'vitest';

import {AuthService, role} from './auth.service';

describe('AuthService', () => {

    const jwtAdmin: string = 'eyJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJ0YWxseS5iYWNrZW5kIiwic3ViIjoidXNlckBtYWlsLmNvbSIsImF1ZCI6InRhbGx5LmFwcCIsImV4cCI6Mzg4MjI3NDEyMiwiaWF0IjoxNzIzNDExMjM1LCJhdXRob3JpdGllcyI6WyJ1c2VyIiwiYWRtaW4iXX0.Hel_2igtI-Em55ErNumKIzKKvHz9CjF_qyKpelgCXm8';
    const jwtUser: string = 'eyJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJ0YWxseS5iYWNrZW5kIiwic3ViIjoidXNlckBtYWlsLmNvbSIsImF1ZCI6InRhbGx5LmFwcCIsImV4cCI6Mzg4MjI3NDEyMiwiaWF0IjoxNzIzNDExMjM1LCJhdXRob3JpdGllcyI6WyJ1c2VyIl19.b_eFKHXDmWbjbcSxyjcLhXq1fWlWbL5YxgCovC-5MzY';

    const issuedAt = 1723411235000;
    const expiresAt = 3882274122000;
    const now = new Date().getTime();
    const expiredLeft = new Date(0, 0, 0, 0, 0, 0, 0).setUTCMilliseconds(expiresAt - now);

    let authService: AuthService;
    let documentSpy: Mock<(arg: string) => void>;

    beforeEach(() => {

        vi.resetAllMocks();

        TestBed.configureTestingModule({});
        authService = TestBed.inject(AuthService);

        documentSpy = vi.spyOn(window.document, 'cookie', 'set')
    });

    it('should be created', () => {
        expect(authService).toBeTruthy();
    });

    it('should set the secure admin JWT', () => {

        authService.setJwt(jwtAdmin, true);

        expect(documentSpy).toHaveBeenCalledExactlyOnceWith(`TALLY_JWT=${jwtAdmin}; SameSite=Strict; Expires=${new Date(expiresAt).toUTCString()}; Secure`);

        expect(authService.getJwt()).eq(jwtAdmin);
        expect(authService.isAuthenticated()).eq(true);
        expect(authService.isAdmin()).eq(true);
        expect(authService.hasRoles([role.user])).eq(true);
        expect(authService.hasRoles([role.admin])).eq(true);
        expect(authService.hasRoles([role.user, role.admin])).eq(true);

        const result = authService.getJwtDetails();

        expect(result.email).toBe('user@mail.com');
        expect(result.issuedAt).toBe(issuedAt);
        expect(result.expiresAt).toBe(expiresAt);
        expect(result.expiresLeft).toBeLessThan(expiredLeft + 1000);
        expect(result.expiresLeft).toBeGreaterThan(expiredLeft - 1000);
        expect(result.authorities).toEqual([role.user, role.admin]);

        authService.removeJwt();
        expect(authService.getJwt()).eq(undefined);
    });

    it('should set the not secure user JWT', () => {

        authService.setJwt(jwtUser, false);

        expect(documentSpy).toHaveBeenCalledExactlyOnceWith(`TALLY_JWT=${jwtUser}; SameSite=Strict; Expires=${new Date(expiresAt).toUTCString()}`);

        expect(authService.getJwt()).eq(jwtUser);
        expect(authService.isAuthenticated()).eq(true);
        expect(authService.isAdmin()).eq(false);
        expect(authService.hasRoles([role.user])).eq(true);
        expect(authService.hasRoles([role.admin])).eq(false);

        const result = authService.getJwtDetails();

        expect(result.email).toBe('user@mail.com');
        expect(result.issuedAt).toBe(issuedAt);
        expect(result.expiresAt).toBe(expiresAt);
        expect(result.expiresLeft).toBeLessThan(expiredLeft + 1000);
        expect(result.expiresLeft).toBeGreaterThan(expiredLeft - 1000);
        expect(result.authorities).toEqual([role.user]);

        authService.removeJwt();
        expect(authService.getJwt()).eq(undefined);
    });
});

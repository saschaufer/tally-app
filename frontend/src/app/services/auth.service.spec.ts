import {TestBed} from '@angular/core/testing';
import {MockProvider} from "ng-mocks";
import {CookieService} from "ngx-cookie-service";

import {AuthService, role} from './auth.service';
import SpyObj = jasmine.SpyObj;

describe('AuthService', () => {

    let authService: AuthService;
    let cookieServiceSpy: SpyObj<CookieService>;
    const jwt = 'eyJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJ0YWxseS5iYWNrZW5kIiwic3ViIjoidXNlciIsImF1ZCI6InRhbGx5LmFwcCIsImV4cCI6MTcxNTg0NzQ4NSwiaWF0IjoxNzE1ODExNDg1LCJhdXRob3JpdGllcyI6WyJ1c2VyIiwiYWRtaW4iXX0.cwSUKr7CWQ8xIGAJCN8lhtSuSbUW5qEPdl1jlLlEAeI';

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [MockProvider(CookieService)],
        });
        authService = TestBed.inject(AuthService);
        cookieServiceSpy = spyOnAllFunctions(TestBed.inject(CookieService));
    });

    it('should be created', () => {
        expect(authService).toBeTruthy();
    });

    it('should confirm the authorities in the JWT', () => {

        cookieServiceSpy.get.and.returnValue(jwt);

        expect(authService.hasRoles([role.user])).toBeTruthy();
        expect(authService.hasRoles([role.admin])).toBeTruthy();
        expect(authService.hasRoles([role.user, role.admin])).toBeTruthy();
    });

    it('should set the not secure JWT', () => {

        cookieServiceSpy.check.and.returnValue(true);

        spyOn(authService, 'isAuthenticated').and.callThrough();

        expect(authService.setJwt(jwt, false)).toBeTruthy();

        expect(cookieServiceSpy.set).toHaveBeenCalledOnceWith('TALLY_JWT', jwt, {
            sameSite: "Strict",
            secure: false,
            expires: new Date(1715847485 * 1000),
        });
        expect(cookieServiceSpy.check).toHaveBeenCalledOnceWith('TALLY_JWT');
        expect(authService.isAuthenticated).toHaveBeenCalled();
    });

    it('should set the secure JWT', () => {

        cookieServiceSpy.check.and.returnValue(true);

        spyOn(authService, 'isAuthenticated').and.callThrough();

        expect(authService.setJwt(jwt, true)).toBeTruthy();

        expect(cookieServiceSpy.set).toHaveBeenCalledOnceWith('TALLY_JWT', jwt, {
            sameSite: "Strict",
            secure: true,
            expires: new Date(1715847485 * 1000),
        });
        expect(cookieServiceSpy.check).toHaveBeenCalledOnceWith('TALLY_JWT');
        expect(authService.isAuthenticated).toHaveBeenCalled();
    });

    it('should get the JWT', () => {

        cookieServiceSpy.get.and.returnValue(jwt);

        expect(authService.getJwt()).toBe(jwt);

        expect(cookieServiceSpy.get).toHaveBeenCalledOnceWith('TALLY_JWT');
    });

    it('should remove the JWT', () => {

        authService.removeJwt();

        expect(cookieServiceSpy.delete).toHaveBeenCalledOnceWith('TALLY_JWT');
    });
});

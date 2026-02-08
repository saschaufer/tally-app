import {TestBed} from '@angular/core/testing';
import {ActivatedRouteSnapshot, Router, RouterStateSnapshot} from '@angular/router';
import {firstValueFrom, of} from "rxjs";
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {routeName} from "../app.routes";
import {AuthService} from "../services/auth.service";
import {authGuard} from "./auth.guard";

describe('AuthGuard', () => {

    const authServiceMock = vi.mockObject(AuthService.prototype);
    const routerMock = vi.mockObject(Router.prototype);

    beforeEach(() => {

        vi.resetAllMocks();

        TestBed.configureTestingModule({
            providers: [
                {provide: AuthService, useValue: authServiceMock},
                {provide: Router, useValue: routerMock}
            ]
        });
    });

    it('should navigate to ' + routeName.settings, () => {

        authServiceMock.isAuthenticated.mockReturnValue(true);
        routerMock.navigate.mockReturnValue(firstValueFrom(of(true)));

        const route = {data: {toLogin: true}} as unknown as ActivatedRouteSnapshot;
        const state = {} as RouterStateSnapshot;
        const result = TestBed.runInInjectionContext(() => authGuard(route, state));

        expect(result).eq(false)

        expect(routerMock.navigate).toHaveBeenCalledExactlyOnceWith(['/' + routeName.settings]);
    });

    it('should not navigate to ' + routeName.settings, () => {

        authServiceMock.isAuthenticated.mockReturnValue(false);

        const route = {data: {toLogin: true}} as unknown as ActivatedRouteSnapshot;
        const state = {} as RouterStateSnapshot;
        const result = TestBed.runInInjectionContext(() => authGuard(route, state));

        expect(result).eq(true);

        expect(routerMock.navigate).not.toHaveBeenCalled();
    });

    it('should navigate to ' + routeName.login, () => {

        authServiceMock.isAuthenticated.mockReturnValue(false);
        routerMock.navigate.mockReturnValue(firstValueFrom(of(true)));

        const route = {data: {toLogin: false}} as unknown as ActivatedRouteSnapshot;
        const state = {} as RouterStateSnapshot;
        const result = TestBed.runInInjectionContext(() => authGuard(route, state));

        expect(result).eq(false);

        expect(routerMock.navigate).toHaveBeenCalledExactlyOnceWith(['/' + routeName.login]);
    });

    it('should not navigate to ' + routeName.login, () => {

        authServiceMock.isAuthenticated.mockReturnValue(true);

        const route = {data: {toLogin: false}} as unknown as ActivatedRouteSnapshot;
        const state = {} as RouterStateSnapshot;
        const result = TestBed.runInInjectionContext(() => authGuard(route, state));

        expect(result).eq(true);

        expect(routerMock.navigate).not.toHaveBeenCalled();
    });
});

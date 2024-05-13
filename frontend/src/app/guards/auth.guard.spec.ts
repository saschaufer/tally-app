import {TestBed} from '@angular/core/testing';
import {ActivatedRouteSnapshot, Router, RouterStateSnapshot} from '@angular/router';
import {MockProvider} from "ng-mocks";
import {firstValueFrom, of} from "rxjs";
import {routeName} from "../app.routes";
import {AuthService} from "../services/auth.service";
import {authGuard} from "./auth.guard";
import SpyObj = jasmine.SpyObj;

describe('AuthGuard', () => {

    let authServiceSpy: SpyObj<AuthService>;
    let routerSpy: SpyObj<Router>;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                MockProvider(AuthService),
                MockProvider(Router)
            ]
        });

        authServiceSpy = spyOnAllFunctions(TestBed.inject(AuthService));
        routerSpy = spyOnAllFunctions(TestBed.inject(Router));
    });

    it('should navigate to ' + routeName.settings, () => {

        authServiceSpy.isAuthenticated.and.returnValue(true);
        routerSpy.navigate.and.callFake(() => firstValueFrom(of(true)));

        const route = {data: {toLogin: true}} as unknown as ActivatedRouteSnapshot;
        const state = {} as RouterStateSnapshot;
        const result = TestBed.runInInjectionContext(() => authGuard(route, state));

        expect(result).toBeFalsy();

        expect(routerSpy.navigate).toHaveBeenCalledOnceWith(['/' + routeName.settings]);
    });

    it('should not navigate to ' + routeName.settings, () => {

        authServiceSpy.isAuthenticated.and.returnValue(false);

        const route = {data: {toLogin: true}} as unknown as ActivatedRouteSnapshot;
        const state = {} as RouterStateSnapshot;
        const result = TestBed.runInInjectionContext(() => authGuard(route, state));

        expect(result).toBeTruthy();

        expect(routerSpy.navigate).not.toHaveBeenCalled();
    });

    it('should navigate to ' + routeName.login, () => {

        authServiceSpy.isAuthenticated.and.returnValue(false);
        routerSpy.navigate.and.callFake(() => firstValueFrom(of(true)));

        const route = {data: {toLogin: false}} as unknown as ActivatedRouteSnapshot;
        const state = {} as RouterStateSnapshot;
        const result = TestBed.runInInjectionContext(() => authGuard(route, state));

        expect(result).toBeFalsy();

        expect(routerSpy.navigate).toHaveBeenCalledOnceWith(['/' + routeName.login]);
    });

    it('should not navigate to ' + routeName.login, () => {

        authServiceSpy.isAuthenticated.and.returnValue(true);

        const route = {data: {toLogin: false}} as unknown as ActivatedRouteSnapshot;
        const state = {} as RouterStateSnapshot;
        const result = TestBed.runInInjectionContext(() => authGuard(route, state));

        expect(result).toBeTruthy();

        expect(routerSpy.navigate).not.toHaveBeenCalled();
    });
});

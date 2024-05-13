import {TestBed} from '@angular/core/testing';
import {ActivatedRouteSnapshot, RouterStateSnapshot} from '@angular/router';
import {MockProvider} from "ng-mocks";
import {AuthService, role} from "../services/auth.service";
import {roleGuard} from "./role.guard";
import SpyObj = jasmine.SpyObj;

describe('RoleGuard', () => {

    let authServiceSpy: SpyObj<AuthService>;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [MockProvider(AuthService)]
        });

        authServiceSpy = spyOnAllFunctions(TestBed.inject(AuthService));
    });

    it('has roles and allows routing', () => {

        authServiceSpy.hasRoles.and.returnValue(true);

        const route = {data: {expectedRoles: [role.user, role.admin]}} as unknown as ActivatedRouteSnapshot;
        const state = {} as RouterStateSnapshot;
        const result = TestBed.runInInjectionContext(() => roleGuard(route, state));

        expect(result).toBeTruthy();

        expect(authServiceSpy.hasRoles).toHaveBeenCalledOnceWith([role.user, role.admin]);
    });

    it('does not have roles and allows routing', () => {

        authServiceSpy.hasRoles.and.returnValue(false);

        const route = {data: {expectedRoles: []}} as unknown as ActivatedRouteSnapshot;
        const state = {} as RouterStateSnapshot;
        const result = TestBed.runInInjectionContext(() => roleGuard(route, state));

        expect(result).toBeFalsy();

        expect(authServiceSpy.hasRoles).toHaveBeenCalledOnceWith([]);
    });
});

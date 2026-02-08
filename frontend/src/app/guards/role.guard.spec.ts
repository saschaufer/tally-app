import {TestBed} from '@angular/core/testing';
import {ActivatedRouteSnapshot, RouterStateSnapshot} from '@angular/router';
import {beforeEach, describe, expect, it} from 'vitest';

import {AuthService, role} from "../services/auth.service";
import {roleGuard} from "./role.guard";

describe('RoleGuard', () => {

    const authServiceMock = vi.mockObject(AuthService.prototype);

    beforeEach(() => {

        vi.resetAllMocks();

        TestBed.configureTestingModule({
            providers: [
                {provide: AuthService, useValue: authServiceMock}
            ]
        });
    });

    it('has roles and allows routing', () => {

        authServiceMock.hasRoles.mockReturnValue(true);

        const route = {data: {expectedRoles: [role.user, role.admin]}} as unknown as ActivatedRouteSnapshot;
        const state = {} as RouterStateSnapshot;
        const result = TestBed.runInInjectionContext(() => roleGuard(route, state));

        expect(result).eq(true);

        expect(authServiceMock.hasRoles).toHaveBeenCalledExactlyOnceWith([role.user, role.admin]);
    });

    it('does not have roles and allows routing', () => {

        authServiceMock.hasRoles.mockReturnValue(false);

        const route = {data: {expectedRoles: []}} as unknown as ActivatedRouteSnapshot;
        const state = {} as RouterStateSnapshot;
        const result = TestBed.runInInjectionContext(() => roleGuard(route, state));

        expect(result).eq(false);

        expect(authServiceMock.hasRoles).toHaveBeenCalledExactlyOnceWith([]);
    });
});

import {ComponentFixture, TestBed} from '@angular/core/testing';
import {Router} from "@angular/router";
import {firstValueFrom, of} from "rxjs";
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {routeName} from "../../../app.routes";
import {AuthService, role} from "../../../services/auth.service";
import {JwtDetails} from "../../../services/models/JwtDetails";

import {LoginDetailsComponent} from './login-details.component';

describe('LoginDetailsComponent', () => {

    let component: LoginDetailsComponent;
    let fixture: ComponentFixture<LoginDetailsComponent>;

    const routerMock = vi.mockObject(Router.prototype);
    const authServiceMock = vi.mockObject(AuthService.prototype);

    const jwtDetails = {
        email: "user@mail.com",
        issuedAt: 1715811485000,
        expiresAt: 1715847485000,
        expiresLeft: 1715847485000 - 1715811485000,
        authorities: [role.user, role.admin]
    } as JwtDetails;

    beforeEach(async () => {

        vi.resetAllMocks();

        authServiceMock.getJwtDetails.mockReturnValueOnce(jwtDetails);

        await TestBed.configureTestingModule({
            imports: [LoginDetailsComponent],
            providers: [
                {provide: Router, useValue: routerMock},
                {provide: AuthService, useValue: authServiceMock},
            ]
        })
            .compileComponents();

        fixture = TestBed.createComponent(LoginDetailsComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    it('should have the JWT details', () => {
        expect(component.jwtDetails).toBe(jwtDetails);
    });

    it('should navigate to ' + routeName.login, () => {

        routerMock.navigate.mockReturnValue(firstValueFrom(of(true)));

        component.onLogout();

        expect(authServiceMock.removeJwt).toHaveBeenCalledTimes(1);
        expect(routerMock.navigate).toHaveBeenCalledExactlyOnceWith(['/' + routeName.login]);
    });
});

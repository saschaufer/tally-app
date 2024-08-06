import {ComponentFixture, TestBed} from '@angular/core/testing';
import {Router} from "@angular/router";
import {MockProvider} from "ng-mocks";
import {firstValueFrom, of} from "rxjs";
import {routeName} from "../../../app.routes";
import {AuthService, role} from "../../../services/auth.service";
import {JwtDetails} from "../../../services/models/JwtDetails";

import {LoginDetailsComponent} from './login-details.component';
import SpyObj = jasmine.SpyObj;

describe('LoginDetailsComponent', () => {

    let component: LoginDetailsComponent;
    let fixture: ComponentFixture<LoginDetailsComponent>;

    let routerSpy: SpyObj<Router>;
    let authServiceSpy: SpyObj<AuthService>;

    const jwtDetails = {
        email: "user@mail.com",
        issuedAt: 1715811485000,
        expiresAt: 1715847485000,
        expiresLeft: 1715847485000 - 1715811485000,
        authorities: [role.user, role.admin]
    } as JwtDetails;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [LoginDetailsComponent],
            providers: [
                MockProvider(AuthService, {
                    getJwtDetails: () => jwtDetails
                }),
                MockProvider(Router)
            ]
        })
            .compileComponents();

        fixture = TestBed.createComponent(LoginDetailsComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();

        routerSpy = spyOnAllFunctions(TestBed.inject(Router));
        authServiceSpy = spyOnAllFunctions(TestBed.inject(AuthService));
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    it('should have the JWT details', () => {
        expect(component.jwtDetails).toBe(jwtDetails);
    });

    it('should navigate to ' + routeName.login, () => {

        routerSpy.navigate.and.callFake(() => firstValueFrom(of(true)));

        component.onLogout();

        expect(authServiceSpy.removeJwt).toHaveBeenCalledTimes(1);
        expect(routerSpy.navigate).toHaveBeenCalledOnceWith(['/' + routeName.login]);
    });
});

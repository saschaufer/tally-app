import {ComponentFixture, TestBed} from '@angular/core/testing';
import {ActivatedRoute, provideRouter} from "@angular/router";
import {of, throwError} from "rxjs";
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {HttpService} from "../../../services/http.service";

import {RegisterConfirmComponent} from './register-confirm.component';

describe('RegisterConfirmComponent', () => {

    let component: RegisterConfirmComponent;
    let fixture: ComponentFixture<RegisterConfirmComponent>;

    const httpServiceMock = vi.mockObject(HttpService.prototype);

    beforeEach(async () => {

        vi.resetAllMocks();

        const email = encodeURIComponent(window.btoa('user@mail.com'));
        const secret = encodeURIComponent(window.btoa('secret'));

        await TestBed.configureTestingModule({
            imports: [RegisterConfirmComponent],
            providers: [
                provideRouter([]),
                {provide: HttpService, useValue: httpServiceMock},
                {provide: ActivatedRoute, useValue: {queryParams: of({e: email, s: secret})}}
            ]
        })
            .compileComponents();

        fixture = TestBed.createComponent(RegisterConfirmComponent);
        component = fixture.componentInstance;
    });

    it('should create and confirm the registration', () => {

        httpServiceMock.postRegisterNewUserConfirm.mockReturnValue(of(undefined));

        fixture.detectChanges();
        expect(component).toBeTruthy();

        expect(httpServiceMock.postRegisterNewUserConfirm).toHaveBeenCalledExactlyOnceWith('user@mail.com', 'secret');
    });

    it('should create and not confirm the registration (confirm the registration failed)', () => {

        httpServiceMock.postRegisterNewUserConfirm.mockReturnValue(
            throwError(() => 'Error on confirming the registration')
        );

        fixture.detectChanges();
        expect(component).toBeTruthy();

        expect(httpServiceMock.postRegisterNewUserConfirm).toHaveBeenCalledExactlyOnceWith('user@mail.com', 'secret');
    });
});

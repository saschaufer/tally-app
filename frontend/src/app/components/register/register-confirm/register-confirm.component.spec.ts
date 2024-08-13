import {ComponentFixture, TestBed} from '@angular/core/testing';
import {ActivatedRoute, provideRouter} from "@angular/router";
import {MockProvider} from "ng-mocks";
import {of, throwError} from "rxjs";
import {HttpService} from "../../../services/http.service";

import {RegisterConfirmComponent} from './register-confirm.component';
import SpyObj = jasmine.SpyObj;

describe('RegisterConfirmComponent', () => {

    let component: RegisterConfirmComponent;
    let fixture: ComponentFixture<RegisterConfirmComponent>;

    let httpServiceSpy: SpyObj<HttpService>;

    beforeEach(async () => {

        const email = encodeURIComponent(window.btoa('user@mail.com'));
        const secret = encodeURIComponent(window.btoa('secret'));

        await TestBed.configureTestingModule({
            imports: [RegisterConfirmComponent],
            providers: [
                MockProvider(HttpService),
                provideRouter([]),
                {provide: ActivatedRoute, useValue: {queryParams: of({e: email, s: secret})}}
            ]
        })
            .compileComponents();

        fixture = TestBed.createComponent(RegisterConfirmComponent);
        component = fixture.componentInstance;

        httpServiceSpy = spyOnAllFunctions(TestBed.inject(HttpService));
    });

    it('should create and confirm the registration', () => {

        httpServiceSpy.postRegisterNewUserConfirm.and.callFake(() => of(undefined));

        fixture.detectChanges();
        expect(component).toBeTruthy();

        expect(httpServiceSpy.postRegisterNewUserConfirm).toHaveBeenCalledOnceWith('user@mail.com', 'secret');
    });

    it('should create and not confirm the registration (confirm the registration failed)', () => {

        httpServiceSpy.postRegisterNewUserConfirm.and.callFake(() => of(undefined));

        httpServiceSpy.postRegisterNewUserConfirm.and.callFake(() =>
            throwError(() => 'Error on confirming the registration')
        );

        fixture.detectChanges();
        expect(component).toBeTruthy();

        expect(httpServiceSpy.postRegisterNewUserConfirm).toHaveBeenCalledOnceWith('user@mail.com', 'secret');
    });
});

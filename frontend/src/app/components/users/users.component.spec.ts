import {HttpErrorResponse} from "@angular/common/http";
import {ComponentFixture, TestBed} from '@angular/core/testing';
import {Big} from "big.js";
import {MockProvider} from "ng-mocks";
import {of, throwError} from "rxjs";
import {HttpService} from "../../services/http.service";
import {GetUsersResponse} from "../../services/models/GetUsersResponse";

import {UsersComponent} from './users.component';
import SpyObj = jasmine.SpyObj;

describe('UsersComponent', () => {

    let component: UsersComponent;
    let fixture: ComponentFixture<UsersComponent>;

    let httpServiceSpy: SpyObj<HttpService>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [UsersComponent],
            providers: [
                MockProvider(HttpService)
            ]
        })
            .compileComponents();

        fixture = TestBed.createComponent(UsersComponent);
        component = fixture.componentInstance;

        httpServiceSpy = spyOnAllFunctions(TestBed.inject(HttpService));
    });

    it('should create', () => {

        //@formatter:off
        httpServiceSpy.getReadUsers.and.callFake(() => of([
          { email: "2@mail", registrationOn: 456, registrationComplete: false, roles: [], accountBalance: Big('45.6')},
          { email: "1@mail", registrationOn: 123, registrationComplete: true, roles: ["1", "2"], accountBalance: Big('12.3')}
        ] as GetUsersResponse[]));
        //@formatter:on

        fixture.detectChanges();

        expect(component).toBeTruthy();

        expect(component.error).toBeUndefined();

        //@formatter:off
        expect(component.users).toEqual([
          { email: "1@mail", registrationOn: 123, registrationComplete: true, roles: ["1", "2"], accountBalance: Big('12.3')},
          { email: "2@mail", registrationOn: 456, registrationComplete: false, roles: [], accountBalance: Big('45.6')}
        ]);
        //@formatter:on
    });

    it('should create (error reading users)', () => {

        httpServiceSpy.getReadUsers.and.callFake(() =>
            throwError(() => new HttpErrorResponse({error: 'Error on reading users'}))
        );

        fixture.detectChanges();

        expect(component).toBeTruthy();

        expect(component.error?.error).toBe('Error on reading users');
        expect(component.users).toBeUndefined();
    });
});

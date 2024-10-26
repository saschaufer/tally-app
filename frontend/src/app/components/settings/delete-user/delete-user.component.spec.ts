import {HttpErrorResponse} from "@angular/common/http";
import {ComponentFixture, TestBed} from '@angular/core/testing';
import {provideRouter, Router} from "@angular/router";
import {MockProvider} from "ng-mocks";
import {firstValueFrom, of, throwError} from "rxjs";
import {routeName} from "../../../app.routes";
import {HttpService} from "../../../services/http.service";

import {DeleteUserComponent} from './delete-user.component';
import Spy = jasmine.Spy;
import SpyObj = jasmine.SpyObj;

describe('DeleteUserComponent', () => {

    let component: DeleteUserComponent;
    let fixture: ComponentFixture<DeleteUserComponent>;

    let httpServiceSpy: SpyObj<HttpService>;

    let routerNavigateSpy: Spy;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [DeleteUserComponent],
            providers: [
                MockProvider(HttpService),
                provideRouter([]),
            ]
        })
            .compileComponents();

        fixture = TestBed.createComponent(DeleteUserComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();

        httpServiceSpy = spyOnAllFunctions(TestBed.inject(HttpService));

        routerNavigateSpy = spyOn(TestBed.inject(Router), 'navigate');
    });

    it('should create', () => {
        expect(component).toBeTruthy();
        expect(component.error).toBeUndefined();
    });

    it('should delete the user and navigate to ' + routeName.login, () => {

        const deleteButton = document.getElementById('#settings.deleteUser.confirmationPrompt.delete')! as HTMLButtonElement;
        const dialog = document.getElementById('#settings.deleteUser.successDeleteUser')! as HTMLDialogElement;

        httpServiceSpy.postDeleteUser.and.callFake(() => of(undefined));
        routerNavigateSpy.and.callFake(() => firstValueFrom(of(true)));

        component.onDeleteAccount()

        expect(httpServiceSpy.postDeleteUser).not.toHaveBeenCalled();

        deleteButton.dispatchEvent(new Event('click'));

        expect(httpServiceSpy.postDeleteUser).toHaveBeenCalledOnceWith();

        expect(routerNavigateSpy).not.toHaveBeenCalled();

        dialog.dispatchEvent(new Event('click'));

        expect(routerNavigateSpy).toHaveBeenCalledOnceWith(['/' + routeName.login]);

        expect(component.error).toBeUndefined();
    })

    it('should not delete the user (delete user canceled)', () => {

        const cancelButton = document.getElementById('#settings.deleteUser.confirmationPrompt.cancel')! as HTMLButtonElement;

        component.onDeleteAccount()

        cancelButton.dispatchEvent(new Event('click'));

        expect(httpServiceSpy.postDeleteUser).not.toHaveBeenCalled();
        expect(routerNavigateSpy).not.toHaveBeenCalled();

        expect(component.error).toBeUndefined();
    })

    it('should not delete the user (delete user failed)', () => {

        const deleteButton = document.getElementById('#settings.deleteUser.confirmationPrompt.delete')! as HTMLButtonElement;
        const dialog = document.getElementById('#settings.deleteUser.errorDeleteUser')! as HTMLDialogElement;

        httpServiceSpy.postDeleteUser.and.callFake(() =>
            throwError(() => new HttpErrorResponse({error: 'Error on deleting user'}))
        );

        component.onDeleteAccount()

        expect(httpServiceSpy.postDeleteUser).not.toHaveBeenCalled();

        deleteButton.dispatchEvent(new Event('click'));

        expect(httpServiceSpy.postDeleteUser).toHaveBeenCalledOnceWith();

        expect(routerNavigateSpy).not.toHaveBeenCalled();

        dialog.dispatchEvent(new Event('click'));

        expect(routerNavigateSpy).not.toHaveBeenCalled();

        expect(component.error?.error).toBe('Error on deleting user');
    })
});

import {HttpErrorResponse} from "@angular/common/http";
import {ComponentFixture, TestBed} from '@angular/core/testing';
import {NavigationExtras, provideRouter, Router} from "@angular/router";
import {firstValueFrom, of, throwError} from "rxjs";
import {beforeEach, describe, expect, it, Mock, vi} from 'vitest';
import {routeName} from "../../../app.routes";
import {HttpService} from "../../../services/http.service";

import {DeleteUserComponent} from './delete-user.component';

describe('DeleteUserComponent', () => {

    let component: DeleteUserComponent;
    let fixture: ComponentFixture<DeleteUserComponent>;

    const httpServiceMock = vi.mockObject(HttpService.prototype);

    let routerNavigateSpy: Mock<(commands: readonly any[], extras?: NavigationExtras) => Promise<boolean>>;

    beforeEach(async () => {

        vi.resetAllMocks();

        await TestBed.configureTestingModule({
            imports: [DeleteUserComponent],
            providers: [
                provideRouter([]),
                {provide: HttpService, useValue: httpServiceMock}
            ]
        })
            .compileComponents();

        fixture = TestBed.createComponent(DeleteUserComponent);
        component = fixture.componentInstance;

        routerNavigateSpy = vi.spyOn(TestBed.inject(Router), 'navigate');

        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
        expect(component.error).toBeUndefined();
    });

    it('should delete the user and navigate to ' + routeName.login, () => {

        const deleteButton = document.getElementById('#settings.deleteUser.confirmationPrompt.delete')! as HTMLButtonElement;
        const dialog: HTMLDialogElement = component.dialogSuccessDeleteUser.nativeElement;

        httpServiceMock.postDeleteUser.mockReturnValue(of(undefined));
        routerNavigateSpy.mockReturnValue(firstValueFrom(of(true)));

        component.onDeleteAccount()

        expect(httpServiceMock.postDeleteUser).not.toHaveBeenCalled();

        deleteButton.dispatchEvent(new Event('click'));

        expect(httpServiceMock.postDeleteUser).toHaveBeenCalledExactlyOnceWith();

        expect(routerNavigateSpy).not.toHaveBeenCalled();

        dialog.dispatchEvent(new Event('click'));

        expect(routerNavigateSpy).toHaveBeenCalledExactlyOnceWith(['/' + routeName.login]);

        expect(component.error).toBeUndefined();
    })

    it('should not delete the user (delete user canceled)', () => {

        const cancelButton = document.getElementById('#settings.deleteUser.confirmationPrompt.cancel')! as HTMLButtonElement;

        component.onDeleteAccount()

        cancelButton.dispatchEvent(new Event('click'));

        expect(httpServiceMock.postDeleteUser).not.toHaveBeenCalled();
        expect(routerNavigateSpy).not.toHaveBeenCalled();

        expect(component.error).toBeUndefined();
    })

    it('should not delete the user (delete user failed)', () => {

        const deleteButton = document.getElementById('#settings.deleteUser.confirmationPrompt.delete')! as HTMLButtonElement;
        const dialog: HTMLDialogElement = component.dialogErrorDeleteUser.nativeElement;

        httpServiceMock.postDeleteUser.mockReturnValue(
            throwError(() => new HttpErrorResponse({error: 'Error on deleting user'}))
        );

        component.onDeleteAccount()

        expect(httpServiceMock.postDeleteUser).not.toHaveBeenCalled();

        deleteButton.dispatchEvent(new Event('click'));

        expect(httpServiceMock.postDeleteUser).toHaveBeenCalledExactlyOnceWith();

        expect(routerNavigateSpy).not.toHaveBeenCalled();

        dialog.dispatchEvent(new Event('click'));

        expect(routerNavigateSpy).not.toHaveBeenCalled();

        expect(component.error?.error).toBe('Error on deleting user');
    })
});

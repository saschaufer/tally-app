import {HttpErrorResponse} from "@angular/common/http";
import {Component, ElementRef, inject, ViewChild} from '@angular/core';
import {Router} from "@angular/router";
import {routeName} from "../../../app.routes";
import {AuthService} from "../../../services/auth.service";
import {HttpService} from "../../../services/http.service";

@Component({
    selector: 'app-delete-user',
    imports: [],
    templateUrl: './delete-user.component.html',
    styles: ``
})
export class DeleteUserComponent {

    @ViewChild('settings.deleteUser.confirmationPrompt', {static: true}) dialogConfirmationPrompt!: ElementRef<HTMLDialogElement>;
    @ViewChild('settings.deleteUser.successDeleteUser', {static: true}) dialogSuccessDeleteUser!: ElementRef<HTMLDialogElement>;
    @ViewChild('settings.deleteUser.errorDeleteUser', {static: true}) dialogErrorDeleteUser!: ElementRef<HTMLDialogElement>;

    protected readonly routeName = routeName;

    private readonly httpService = inject(HttpService);
    private readonly authService = inject(AuthService);
    private readonly router = inject(Router);

    error: HttpErrorResponse | undefined;

    onDeleteAccount() {
        this.openDialogConfirmation();
    }

    deleteAccount() {

        this.httpService.postDeleteUser()
            .subscribe({
                next: () => {
                    console.info('Delete Account successful.');
                    this.openDialogSuccess();

                },
                error: (error: HttpErrorResponse) => {
                    console.error('Error deleting account.');
                    console.error(error);
                    this.error = error;
                    this.openDialog(this.dialogErrorDeleteUser.nativeElement);
                }
            });
    }

    logout() {
        this.authService.removeJwt();
        this.router.navigate(["/" + routeName.login]).then();
    }

    openDialogConfirmation() {
        const dialog: HTMLDialogElement = this.dialogConfirmationPrompt.nativeElement;
        const deleteButton = document.getElementById('#settings.deleteUser.confirmationPrompt.delete')! as HTMLButtonElement;
        const cancelButton = document.getElementById('#settings.deleteUser.confirmationPrompt.cancel')! as HTMLButtonElement;
        deleteButton.addEventListener('click', () => {
            this.deleteAccount();
            dialog.close();
        });
        cancelButton.addEventListener('click', () => {
            dialog.close();
        });
        dialog.showModal();
    }

    openDialogSuccess() {
        const dialog: HTMLDialogElement = this.dialogSuccessDeleteUser.nativeElement;
        dialog.addEventListener('click', () => {
            dialog.close();
            this.logout();
        });
        dialog.showModal();
    }

    openDialog(dialog: HTMLDialogElement) {
        dialog.addEventListener('click', () => {
            dialog.close();
        });
        dialog.showModal();
    }
}

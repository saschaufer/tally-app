import {HttpErrorResponse} from "@angular/common/http";
import {Component, inject, NgZone} from '@angular/core';
import {Router} from "@angular/router";
import {routeName} from "../../../app.routes";
import {AuthService} from "../../../services/auth.service";
import {HttpService} from "../../../services/http.service";

@Component({
    selector: 'app-delete-user',
    standalone: true,
    imports: [],
    templateUrl: './delete-user.component.html',
    styles: ``
})
export class DeleteUserComponent {

    protected readonly routeName = routeName;

    private httpService = inject(HttpService);
    private authService = inject(AuthService);
    private router = inject(Router);
    private zone = inject(NgZone);

    error: HttpErrorResponse | undefined;

    onDeleteAccount() {

        const dialog = document.getElementById('#settings.deleteUser.confirmationPrompt')! as HTMLDialogElement;
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

    deleteAccount() {

        this.httpService.postDeleteUser()
            .subscribe({
                next: () => {
                    console.info('Delete Account successful.');
                    const dialog = document.getElementById('#settings.deleteUser.successDeleteUser')! as HTMLDialogElement;
                    dialog.addEventListener('click', () => {
                        dialog.close();
                        this.logout();
                    });
                    dialog.showModal();
                },
                error: (error: HttpErrorResponse) => {
                    console.error('Error deleting account.');
                    console.error(error);
                    this.error = error;
                    this.openDialog('#settings.deleteUser.errorDeleteUser');
                }
            });
    }

    logout() {
        this.authService.removeJwt();
        this.zone.run(() =>
            this.router.navigate(["/" + routeName.login]).then()
        ).then();
    }

    openDialog(id: string) {
        const dialog = document.getElementById(id)! as HTMLDialogElement;
        dialog.addEventListener('click', () => {
            dialog.close();
        });
        dialog.showModal();
    }
}

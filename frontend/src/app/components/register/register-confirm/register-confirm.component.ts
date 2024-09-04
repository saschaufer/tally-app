import {HttpErrorResponse} from "@angular/common/http";
import {Component, inject} from '@angular/core';
import {ActivatedRoute, RouterLink} from "@angular/router";
import {routeName} from "../../../app.routes";
import {HttpService} from "../../../services/http.service";

@Component({
    selector: 'app-register-confirm',
    standalone: true,
    imports: [
        RouterLink
    ],
    templateUrl: './register-confirm.component.html',
    styles: ``
})
export class RegisterConfirmComponent {

    protected readonly routeName = routeName;

    private activatedRoute = inject(ActivatedRoute);
    private httpService = inject(HttpService);

    error: HttpErrorResponse | undefined;

    ngOnInit(): void {
        this.activatedRoute.queryParams.subscribe(params => {

            const base64Email = decodeURIComponent(params['e']);
            const email = window.atob(base64Email);

            const base64Secret = decodeURIComponent(params['s']);
            const secret = window.atob(base64Secret);

            this.httpService.postRegisterNewUserConfirm(email, secret)
                .subscribe({
                    next: () => console.info("Confirm registration successful"),
                    error: (error: HttpErrorResponse) => {
                        console.error('Error register.');
                        console.error(error);
                        this.error = error;
                        this.openDialog('#register.registerConfirm.errorRegisterConfirm');
                    }
                });
        });
    }

    openDialog(id: string) {
        const dialog = document.getElementById(id)! as HTMLDialogElement;
        dialog.addEventListener('click', () => {
            dialog.close();
        });
        dialog.showModal();
    }
}

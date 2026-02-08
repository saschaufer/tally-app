import {HttpErrorResponse} from "@angular/common/http";
import {Component, ElementRef, inject, ViewChild} from '@angular/core';
import {ActivatedRoute, RouterLink} from "@angular/router";
import {routeName} from "../../../app.routes";
import {HttpService} from "../../../services/http.service";

@Component({
    selector: 'app-register-confirm',
    imports: [
        RouterLink
    ],
    templateUrl: './register-confirm.component.html',
    styles: ``
})
export class RegisterConfirmComponent {

    @ViewChild('register.registerConfirm.errorRegisterConfirm', {static: true}) dialog!: ElementRef<HTMLDialogElement>;

    protected readonly routeName = routeName;

    private readonly activatedRoute = inject(ActivatedRoute);
    private readonly httpService = inject(HttpService);

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
                        this.openDialog(this.dialog.nativeElement);
                    }
                });
        });
    }

    openDialog(dialog: HTMLDialogElement) {
        dialog.addEventListener('click', () => {
            dialog.close();
        });
        dialog.showModal();
    }
}

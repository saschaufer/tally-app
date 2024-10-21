import {DatePipe, NgClass, NgForOf, NgIf} from "@angular/common";
import {HttpErrorResponse} from "@angular/common/http";
import {Component, inject} from '@angular/core';
import {RouterLink} from "@angular/router";
import {Big} from "big.js";
import {HttpService} from "../../services/http.service";
import {GetUsersResponse} from "../../services/models/GetUsersResponse";

@Component({
    selector: 'app-users',
    standalone: true,
    imports: [
        NgForOf,
        NgIf,
        RouterLink,
        DatePipe,
        NgClass
    ],
    templateUrl: './users.component.html',
    styles: ``
})
export class UsersComponent {

    private httpService = inject(HttpService);

    users: GetUsersResponse[] | undefined;

    error: HttpErrorResponse | undefined;

    ngOnInit(): void {

        this.httpService.getReadUsers()
            .subscribe({
                next: users => {
                    console.info("Users read.");
                    users.sort((a, b) => a.email.localeCompare(b.email));
                    this.users = users;
                },
                error: (error: HttpErrorResponse) => {
                    console.error('Error reading users.');
                    console.error(error);
                    this.error = error;
                    this.openDialog('#users.errorReadingUsers');
                }
            });
    }

    isNegative(amount: Big) {
        return Big(amount).lt(Big('0.0'));
    }

    openDialog(id: string) {
        const dialog = document.getElementById(id)! as HTMLDialogElement;
        dialog.addEventListener('click', () => {
            dialog.close();
        });
        dialog.showModal();
    }
}

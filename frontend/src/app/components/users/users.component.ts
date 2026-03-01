import {DatePipe, NgClass} from "@angular/common";
import {HttpErrorResponse} from "@angular/common/http";
import {ChangeDetectorRef, Component, ElementRef, inject, ViewChild} from '@angular/core';
import {Big} from "big.js";
import {HttpService} from "../../services/http.service";
import {GetUsersResponse} from "../../services/models/GetUsersResponse";
import {PropertiesService} from "../../services/properties.service";

@Component({
    selector: 'app-users',
    imports: [
        DatePipe,
        NgClass
    ],
    templateUrl: './users.component.html',
    styles: ``
})
export class UsersComponent {

    @ViewChild('users.errorReadingUsers', {static: true}) dialog!: ElementRef<HTMLDialogElement>;

    private readonly httpService = inject(HttpService);
    private readonly cdr = inject(ChangeDetectorRef);
    private readonly propertiesService = inject(PropertiesService);

    users: GetUsersResponse[] = [];
    currency = this.propertiesService.getProperties().currency;

    error: HttpErrorResponse | undefined;

    ngOnInit(): void {

        this.httpService.getReadUsers()
            .subscribe({
                next: users => {
                    console.info("Users read: " + users.length + " users found.");
                    users.sort((a, b) => a.email.localeCompare(b.email));
                    this.users = users;
                    this.cdr.detectChanges();
                },
                error: (error: HttpErrorResponse) => {
                    console.error('Error reading users.');
                    console.error(error);
                    this.error = error;
                    this.openDialog(this.dialog.nativeElement);
                }
            });
    }

    isNegative(amount: Big) {
        return Big(amount).lt(Big('0.0'));
    }

    openDialog(dialog: HTMLDialogElement) {
        dialog.addEventListener('click', () => {
            dialog.close();
        });
        dialog.showModal();
    }
}

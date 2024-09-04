import {HttpRequest} from "@angular/common/http";
import {Injectable} from "@angular/core";
import {BehaviorSubject} from "rxjs";

@Injectable({
    providedIn: 'root'
})
export class HttpLoaderService {

    private readonly requests: HttpRequest<any>[] = [];

    public isLoading = new BehaviorSubject(false);

    public addRequest(req: HttpRequest<any>) {
        console.info('Add request to loader service.');
        this.requests.push(req);
        this.isLoading.next(true);
    }

    public removeRequest(req: HttpRequest<any>): void {
        const i = this.requests.indexOf(req);
        if (i >= 0) {
            this.requests.splice(i, 1); // Delete element on index i.
        }
        this.isLoading.next(this.requests.length > 0);
        console.info('Removed request from loader service. Remaining requests: ' + this.requests.length);
    }
}

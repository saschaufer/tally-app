import {HttpHandlerFn, HttpInterceptorFn, HttpRequest, HttpResponse} from '@angular/common/http';
import {inject} from '@angular/core';
import {Observable} from "rxjs";
import {HttpLoaderService} from "./http-loader.service";

export const httpLoaderInterceptor: HttpInterceptorFn = (req: HttpRequest<unknown>, next: HttpHandlerFn) => {//: Observable<HttpEvent<unknown>> => {

    const httpLoaderService = inject(HttpLoaderService);

    httpLoaderService.addRequest(req);

    return new Observable(observer => {
        const subscription = next(req)
            .subscribe({
                next: (value) => {
                    if (value instanceof HttpResponse) {
                        httpLoaderService.removeRequest(req);
                        observer.next(value);
                    }
                },
                error: (error) => {
                    httpLoaderService.removeRequest(req);
                    observer.error(error);
                }
            })

        // teardown logic in case of cancelled requests
        return () => {
            httpLoaderService.removeRequest(req);
            subscription.unsubscribe();
        };
    });
};

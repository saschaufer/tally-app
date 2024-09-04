import {HttpRequest} from "@angular/common/http";
import {TestBed} from '@angular/core/testing';
import {HttpLoaderService} from "./http-loader.service";

describe('HttpLoaderService', () => {

    let httpLoaderService: HttpLoaderService;

    beforeEach(() => {
        httpLoaderService = TestBed.inject(HttpLoaderService);
    });

    it('should be created', () => {
        expect(httpLoaderService).toBeTruthy();
    });

    it('should switch loading on and off', () => {

        const request1 = new HttpRequest('GET', 'url');
        const request2 = new HttpRequest('GET', 'url');

        expect(httpLoaderService.isLoading.value).toBeFalse();

        httpLoaderService.addRequest(request1);
        expect(httpLoaderService.isLoading.value).toBeTrue();

        httpLoaderService.addRequest(request2);
        expect(httpLoaderService.isLoading.value).toBeTrue();

        httpLoaderService.removeRequest(request1);
        expect(httpLoaderService.isLoading.value).toBeTrue();

        httpLoaderService.removeRequest(request2);
        expect(httpLoaderService.isLoading.value).toBeFalse();
    });
});

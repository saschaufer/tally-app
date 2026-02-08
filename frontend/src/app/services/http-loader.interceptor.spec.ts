import {HttpRequest, provideHttpClient, withInterceptors} from "@angular/common/http";
import {HttpTestingController, provideHttpClientTesting} from "@angular/common/http/testing";
import {TestBed} from "@angular/core/testing";
import {afterEach, beforeEach, describe, expect, it, Mock, vi} from 'vitest';
import {httpLoaderInterceptor} from "./http-loader.interceptor";
import {HttpLoaderService} from "./http-loader.service";
import {HttpService} from "./http.service";

describe('HttpLoaderInterceptor', () => {

    let httpService: HttpService;
    let httpTestingController: HttpTestingController;

    let httpLoaderServiceSpyAddRequest: Mock<(req: HttpRequest<any>) => void>;
    let httpLoaderServiceSpyRemoveRequest: Mock<(req: HttpRequest<any>) => void>;

    beforeEach(() => {

        vi.resetAllMocks();

        TestBed.configureTestingModule({
            imports: [],
            providers: [
                HttpService,
                HttpLoaderService,
                provideHttpClient(withInterceptors([httpLoaderInterceptor])),
                provideHttpClientTesting()
            ]
        });

        httpService = TestBed.inject(HttpService);
        httpTestingController = TestBed.inject(HttpTestingController);

        httpLoaderServiceSpyAddRequest = vi.spyOn(TestBed.inject(HttpLoaderService), 'addRequest');
        httpLoaderServiceSpyRemoveRequest = vi.spyOn(TestBed.inject(HttpLoaderService), 'removeRequest');
    });

    afterEach(() => {
        httpTestingController.verify(); // No open request
    })

    it('should call the loader service', () => {

        expect(httpLoaderServiceSpyAddRequest).not.toHaveBeenCalled();
        expect(httpLoaderServiceSpyRemoveRequest).not.toHaveBeenCalled();

        httpService.postLogin('mail', 'password').subscribe(response => {
            expect(response).toBeTruthy();
        });

        expect(httpLoaderServiceSpyAddRequest).toHaveBeenCalled();
        expect(httpLoaderServiceSpyRemoveRequest).not.toHaveBeenCalled();

        const httpRequest = httpTestingController.expectOne('/login');
        httpRequest.flush({}); // Response

        expect(httpLoaderServiceSpyAddRequest).toHaveBeenCalled();
        expect(httpLoaderServiceSpyRemoveRequest).toHaveBeenCalled();
    });
});

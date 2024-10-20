import {provideHttpClient, withInterceptors} from "@angular/common/http";
import {HttpTestingController, provideHttpClientTesting} from "@angular/common/http/testing";
import {TestBed} from "@angular/core/testing";
import {MockProvider} from "ng-mocks";
import {httpLoaderInterceptor} from "./http-loader.interceptor";
import {HttpLoaderService} from "./http-loader.service";
import {HttpService} from "./http.service";
import SpyObj = jasmine.SpyObj;

describe('HttpLoaderInterceptor', () => {

    let httpService: HttpService;
    let httpTestingController: HttpTestingController;

    let httpLoaderServiceSpy: SpyObj<HttpLoaderService>;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [],
            providers: [
                HttpService,
                MockProvider(HttpLoaderService),
                provideHttpClient(withInterceptors([httpLoaderInterceptor])),
                provideHttpClientTesting()
            ]
        });

        httpService = TestBed.inject(HttpService);
        httpTestingController = TestBed.inject(HttpTestingController);

        httpLoaderServiceSpy = spyOnAllFunctions(TestBed.inject(HttpLoaderService));
    });

    afterEach(() => {
        httpTestingController.verify(); // No open request
    })

    it('should call the loader service', () => {

        expect(httpLoaderServiceSpy.addRequest).not.toHaveBeenCalled();
        expect(httpLoaderServiceSpy.removeRequest).not.toHaveBeenCalled();

        httpService.postLogin('mail', 'password').subscribe(response => {
            expect(response).toBeTruthy();
        });

        expect(httpLoaderServiceSpy.addRequest).toHaveBeenCalled();
        expect(httpLoaderServiceSpy.removeRequest).not.toHaveBeenCalled();

        const httpRequest = httpTestingController.expectOne('/login');
        httpRequest.flush({}); // Response

        expect(httpLoaderServiceSpy.addRequest).toHaveBeenCalled();
        expect(httpLoaderServiceSpy.removeRequest).toHaveBeenCalled();
    });
});

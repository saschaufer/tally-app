import {HttpClient} from "@angular/common/http";
import {TestBed} from '@angular/core/testing';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {HttpService} from './http.service';

describe('HttpService', () => {

    let service: HttpService;

    const httpClientMock = vi.mockObject(HttpClient.prototype);

    beforeEach(() => {

        vi.resetAllMocks();

        TestBed.configureTestingModule({
            providers: [{provide: HttpClient, useValue: httpClientMock}]
        });
        service = TestBed.inject(HttpService);
    });

    it('should be created', () => {
        expect(service).toBeTruthy();
    });
});

import {TestBed} from '@angular/core/testing';
import {beforeEach, describe, expect, it} from 'vitest';
import {Properties} from "./models/LoginResponse";
import {PropertiesService} from './properties.service';

describe('PropertiesService', () => {

    let propertiesService: PropertiesService;

    beforeEach(() => {
        TestBed.configureTestingModule({});
        propertiesService = TestBed.inject(PropertiesService);
    });

    it('should be created', () => {
        expect(propertiesService).toBeTruthy();
    });

    it('should set and read the properties', () => {

        propertiesService.setProperties({currency: '€'} as Properties);

        const out1: Properties = propertiesService.getProperties();
        expect(out1).toEqual({currency: '€'} as Properties);

        propertiesService.clear();

        const out2: Properties = propertiesService.getProperties();
        expect(out2).toEqual({});
    });
});

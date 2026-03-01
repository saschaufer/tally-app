import {Injectable} from '@angular/core';
import {Properties} from "./models/LoginResponse";

@Injectable({
    providedIn: 'root',
})
export class PropertiesService {

    setProperties(properties: Properties) {
        window.localStorage.setItem('properties', JSON.stringify(properties));
    }

    getProperties(): Properties {
        return JSON.parse(window.localStorage.getItem('properties') ?? '{}');
    }

    clear() {
        window.localStorage.clear();
    }
}

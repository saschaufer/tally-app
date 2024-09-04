import {provideHttpClient, withInterceptors} from "@angular/common/http";
import {ApplicationConfig} from '@angular/core';
import {provideRouter, withHashLocation} from '@angular/router';

import {routes} from './app.routes';
import {httpLoaderInterceptor} from "./services/http-loader.interceptor";

export const appConfig: ApplicationConfig = {
    providers: [
        provideRouter(routes, withHashLocation()),
        provideHttpClient(withInterceptors([httpLoaderInterceptor]))
    ]
};

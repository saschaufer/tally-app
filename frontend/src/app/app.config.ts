import {provideHttpClient, withInterceptors} from "@angular/common/http";
import {ApplicationConfig, provideBrowserGlobalErrorListeners} from '@angular/core';
import {provideRouter, withHashLocation} from '@angular/router';

import {routes} from './app.routes';
import {httpLoaderInterceptor} from "./services/http-loader.interceptor";

export const appConfig: ApplicationConfig = {
    providers: [
        provideBrowserGlobalErrorListeners(),
        provideRouter(routes, withHashLocation()),
        provideHttpClient(withInterceptors([httpLoaderInterceptor]))
    ]
};

import {inject, NgZone} from "@angular/core";
import {CanActivateFn, Router} from '@angular/router';
import {routeName} from "../app.routes";
import {AuthService} from "../services/auth.service";

export const authGuard: CanActivateFn = (route) => {

    const authService = inject(AuthService);
    const router = inject(Router);
    const zone = inject(NgZone);

    const toLogin = !!route.data['toLogin'];

    if (toLogin && authService.isAuthenticated()) {
        zone.run(() =>
            router.navigate(['/' + routeName.settings]).then()
        ).then();
        return false;
    }

    if (!toLogin && !authService.isAuthenticated()) {
        zone.run(() =>
            router.navigate(['/' + routeName.login]).then()
        ).then();
        return false;
    }

    return true;
};

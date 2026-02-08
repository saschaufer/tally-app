import {inject} from "@angular/core";
import {CanActivateFn, Router} from '@angular/router';
import {routeName} from "../app.routes";
import {AuthService} from "../services/auth.service";

export const authGuard: CanActivateFn = (route) => {

    const authService = inject(AuthService);
    const router = inject(Router);

    const toLogin = !!route.data['toLogin'];

    if (toLogin && authService.isAuthenticated()) {
        router.navigate(['/' + routeName.settings]).then();
        return false;
    }

    if (!toLogin && !authService.isAuthenticated()) {
        router.navigate(['/' + routeName.login]).then();
        return false;
    }

    return true;
};

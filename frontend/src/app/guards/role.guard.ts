import {inject} from "@angular/core";
import {CanActivateFn} from '@angular/router';
import {AuthService, role} from "../services/auth.service";

export const roleGuard: CanActivateFn = (route) => {

    const authService = inject(AuthService);

    const expectedRoles = route.data['expectedRoles'] as role[];

    if (!authService.hasRoles(expectedRoles)) {
        console.error('Not allowed.');
        return false;
    }

    return true;
};

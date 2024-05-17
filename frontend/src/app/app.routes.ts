import {Routes} from '@angular/router';
import {LoginComponent} from "./components/login/login.component";
import {RegisterComponent} from "./components/register/register.component";
import {SettingsComponent} from "./components/settings/settings.component";
import {authGuard} from "./guards/auth.guard";
import {roleGuard} from "./guards/role.guard";
import {role} from "./services/auth.service";

export enum routeName {
    login = 'login',
    register = 'register',
    settings = 'settings',
}

export const routes: Routes = [
    {
        path: '',
        pathMatch: 'full',
        redirectTo: routeName.settings
    },
    {
        path: routeName.login,
        pathMatch: 'full',
        canActivate: [authGuard], data: {toLogin: true},
        component: LoginComponent
    },
    {
        path: routeName.register,
        pathMatch: 'full',
        component: RegisterComponent
    },
    {
        path: routeName.settings,
        pathMatch: 'full',
        canActivate: [authGuard, roleGuard], data: {expectedRoles: [role.user]},
        component: SettingsComponent
    },
    {
        path: '**',
        pathMatch: 'full',
        redirectTo: routeName.settings
    }
];

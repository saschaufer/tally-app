import {Routes} from '@angular/router';
import {LoginComponent} from "./components/login/login.component";
import {PaymentDeleteComponent} from "./components/payments/payment-delete/payment-delete.component";
import {PaymentNewComponent} from "./components/payments/payment-new/payment-new.component";
import {PaymentsComponent} from "./components/payments/payments.component";
import {ProductEditComponent} from "./components/products/product-edit/product-edit.component";
import {ProductNewComponent} from "./components/products/product-new/product-new.component";
import {ProductsComponent} from "./components/products/products.component";
import {PurchaseDeleteComponent} from "./components/purchases/purchase-delete/purchase-delete.component";
import {PurchaseNewComponent} from "./components/purchases/purchase-new/purchase-new.component";
import {PurchasesComponent} from "./components/purchases/purchases.component";
import {QrComponent} from "./components/qr/qr.component";
import {RegisterConfirmComponent} from "./components/register/register-confirm/register-confirm.component";
import {RegisterComponent} from "./components/register/register.component";
import {ResetPasswordComponent} from "./components/reset-password/reset-password.component";
import {SettingsComponent} from "./components/settings/settings.component";
import {UsersComponent} from "./components/users/users.component";
import {authGuard} from "./guards/auth.guard";
import {roleGuard} from "./guards/role.guard";
import {role} from "./services/auth.service";

export enum routeName {
    login = 'login',
    payments = 'payments',
    payments_delete = 'payments/delete',
    payments_new = 'payments/new',
    products = 'products',
    products_edit = 'products/edit',
    products_new = 'products/new',
    purchases = 'purchases',
    purchases_delete = 'purchases/delete',
    purchases_new = 'purchases/new',
    qr = 'qr',
    register = 'register',
    register_confirm = 'register/confirm',
    reset_password = 'reset-password',
    settings = 'settings',
    users = 'users'
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
        path: routeName.payments,
        pathMatch: "full",
        canActivate: [authGuard, roleGuard], data: {expectedRoles: [role.user]},
        component: PaymentsComponent
    },
    {
        path: routeName.payments_delete + '/:payment',
        pathMatch: "full",
        canActivate: [authGuard, roleGuard], data: {expectedRoles: [role.user]},
        component: PaymentDeleteComponent
    },
    {
        path: routeName.payments_new,
        pathMatch: "full",
        canActivate: [authGuard, roleGuard], data: {expectedRoles: [role.user]},
        component: PaymentNewComponent
    },
    {
        path: routeName.products,
        pathMatch: 'full',
        canActivate: [authGuard, roleGuard], data: {expectedRoles: [role.admin]},
        component: ProductsComponent
    },
    {
        path: routeName.products_edit + '/:product',
        pathMatch: 'full',
        canActivate: [authGuard, roleGuard], data: {expectedRoles: [role.admin]},
        component: ProductEditComponent
    },
    {
        path: routeName.products_new,
        pathMatch: 'full',
        canActivate: [authGuard, roleGuard], data: {expectedRoles: [role.admin]},
        component: ProductNewComponent
    },
    {
        path: routeName.purchases,
        pathMatch: "full",
        canActivate: [authGuard, roleGuard], data: {expectedRoles: [role.user]},
        component: PurchasesComponent
    },
    {
        path: routeName.purchases_delete + '/:purchase',
        pathMatch: "full",
        canActivate: [authGuard, roleGuard], data: {expectedRoles: [role.user]},
        component: PurchaseDeleteComponent
    },
    {
        path: routeName.purchases_new,
        pathMatch: "full",
        canActivate: [authGuard, roleGuard], data: {expectedRoles: [role.user]},
        component: PurchaseNewComponent
    },
    {
        path: routeName.qr + '/:productId',
        pathMatch: "full",
        canActivate: [authGuard, roleGuard], data: {expectedRoles: [role.user]},
        component: QrComponent
    },
    {
        path: routeName.register,
        pathMatch: 'full',
        component: RegisterComponent
    },
    {
        path: routeName.register_confirm,
        pathMatch: 'full',
        component: RegisterConfirmComponent
    },
    {
        path: routeName.reset_password,
        pathMatch: 'full',
        component: ResetPasswordComponent
    },
    {
        path: routeName.settings,
        pathMatch: 'full',
        canActivate: [authGuard, roleGuard], data: {expectedRoles: [role.user]},
        component: SettingsComponent
    },
    {
        path: routeName.users,
        pathMatch: 'full',
        canActivate: [authGuard, roleGuard], data: {expectedRoles: [role.admin]},
        component: UsersComponent
    },
    {
        path: '**',
        pathMatch: 'full',
        redirectTo: routeName.settings
    }
];

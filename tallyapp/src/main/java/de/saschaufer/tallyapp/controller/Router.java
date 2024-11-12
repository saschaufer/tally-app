package de.saschaufer.tallyapp.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

@Slf4j
@Configuration(proxyBeanMethods = false)
public class Router {

    @Bean
    public RouterFunction<ServerResponse> route(final Handler handler) {
        return RouterFunctions
                .route()
                .POST("/login", handler::postLogin)
                .POST("/register", handler::postRegisterNewUser)
                .POST("/register/confirm", handler::postRegisterNewUserConfirm)
                .POST("/reset-password", handler::postResetPassword)
                .POST("/settings/change-password", handler::postChangePassword)
                .POST("/settings/change-invitation-code", handler::postChangeInvitationCode)
                .GET("/users", handler::getReadAllUsers)
                .POST("/delete-user", handler::postDeleteUser)

                .GET("/products", handler::getReadProducts)
                .POST("/products/read-product", handler::postReadProduct)
                .POST("/products/create-product", handler::postCreateProduct)
                .POST("/products/update-product", handler::postUpdateProduct)
                .POST("/products/delete-product", handler::postDeleteProduct)
                .POST("/products/update-price", handler::postUpdateProductPrice)

                .GET("/purchases", handler::getReadPurchases)
                .POST("/purchases/create-purchase", handler::postCreatePurchase)
                .POST("/purchases/delete-purchase", handler::postDeletePurchase)

                .GET("/payments", handler::getReadPayments)
                .POST("/payments/create-payment", handler::postCreatePayment)
                .POST("/payments/delete-payment", handler::postDeletePayment)

                .GET("/account-balance", handler::getReadAccountBalance)

                .build();
    }
}

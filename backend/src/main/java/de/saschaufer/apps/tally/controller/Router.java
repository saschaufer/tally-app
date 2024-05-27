package de.saschaufer.apps.tally.controller;

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
                .POST("/settings/change-password", handler::postChangePassword)
                .POST("/settings/change-invitation-code", handler::postChangeInvitationCode)

                .GET("/products", handler::getReadProducts)
                .POST("/products/read-product", handler::postReadProduct)
                .POST("/products/create-product", handler::postCreateProduct)
                .POST("/products/update-product", handler::postUpdateProduct)
                .POST("/products/update-price", handler::postUpdateProductPrice)

                .GET("/purchases", handler::getReadPurchases)
                .POST("/purchases/create-purchase", handler::postCreatePurchase)
                .POST("/purchases/delete-purchase", handler::postDeletePurchase)

                .build();
    }
}

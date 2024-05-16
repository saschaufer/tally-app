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
                .GET("/none", handler::getNone)
                .GET("/user", handler::getUser)
                .GET("/admin", handler::getAdmin)
                .GET("/user-admin", handler::getUseradmin)
                .build();
    }
}
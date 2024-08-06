package de.saschaufer.apps.tally.controller;

import de.saschaufer.apps.tally.config.security.SecurityConfigSetup;
import de.saschaufer.apps.tally.controller.dto.*;
import de.saschaufer.apps.tally.persistence.dto.User;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static de.saschaufer.apps.tally.persistence.dto.User.Role.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RouterTest extends SecurityConfigSetup {

    @Test
    void postLogin_positive() {

        doReturn(new PostLoginResponse("jwt", true)).when(userDetailsService).createJwtToken(any(User.class));

        webClient.post().uri("/login")
                .header(HttpHeaders.AUTHORIZATION, "Basic " + credentials(USER, true))
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody(PostLoginResponse.class).isEqualTo(new PostLoginResponse("jwt", true));

        verify(userDetailsService, times(1)).findByUsername(any(String.class));
        verify(userDetailsService, times(1)).createJwtToken(any(User.class));

        final ArgumentCaptor<User> argumentCaptor = ArgumentCaptor.forClass(User.class);
        verify(userDetailsService).createJwtToken(argumentCaptor.capture());

        final User user = argumentCaptor.getValue();

        assertThat(user.getId(), is(2L));
        assertThat(user.getUsername(), is(USER));
        assertThat(user.getPassword(), notNullValue());
        assertThat(user.getRoles(), is(USER));
    }

    @Test
    void postRegisterNewUser_positive() {

        doReturn(Mono.just(new User())).when(userDetailsService).createUser(any(String.class), any(String.class), ArgumentMatchers.<String>anyList());

        webClient.post().uri("/register")
                .header(HttpHeaders.AUTHORIZATION, "Basic " + credentials(INVITATION, true))
                .body(Mono.just(new PostRegisterNewUserRequest("test-user@mail.com", "test-password")), PostRegisterNewUserRequest.class)
                .exchange()
                .expectStatus().isOk()
                .expectBody().isEmpty();

        verify(userDetailsService, times(1)).findByUsername(any(String.class));
        verify(userDetailsService, times(1)).createUser(any(String.class), any(String.class), ArgumentMatchers.<String>anyList());

        final ArgumentCaptor<String> argumentCaptorUsername = ArgumentCaptor.forClass(String.class);
        final ArgumentCaptor<String> argumentCaptorPassword = ArgumentCaptor.forClass(String.class);

        @SuppressWarnings("unchecked") // Can't get any better
        final ArgumentCaptor<? extends List<String>> argumentCaptorRoles = ArgumentCaptor.forClass((Class) List.class);

        verify(userDetailsService).createUser(argumentCaptorUsername.capture(), argumentCaptorPassword.capture(), argumentCaptorRoles.capture());

        assertThat(argumentCaptorUsername.getValue(), is("test-user@mail.com"));
        assertThat(argumentCaptorPassword.getValue(), is("test-password"));
        assertThat(argumentCaptorRoles.getValue(), containsInAnyOrder(USER));
    }

    @Test
    void postRegisterNewUser_negative_NoBody() {

        webClient.post().uri("/register")
                .header(HttpHeaders.AUTHORIZATION, "Basic " + credentials(INVITATION, true))
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentType(MediaType.TEXT_PLAIN)
                .expectBody(String.class).isEqualTo("Body required");

        verify(userDetailsService, times(1)).findByUsername(any(String.class));
        verify(userDetailsService, times(0)).createUser(any(String.class), any(String.class), ArgumentMatchers.<String>anyList());
    }

    @Test
    void postRegisterNewUser_negative_BodyWrongType() {

        webClient.post().uri("/register")
                .header(HttpHeaders.AUTHORIZATION, "Basic " + credentials(INVITATION, true))
                .body(Mono.just("Wrong Type"), String.class)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .expectHeader().contentType(MediaType.TEXT_PLAIN)
                .expectBody(String.class).value(stringContainsInOrder("Content type '", "' not supported. Supported: '"));

        verify(userDetailsService, times(1)).findByUsername(any(String.class));
        verify(userDetailsService, times(0)).createUser(any(String.class), any(String.class), ArgumentMatchers.<String>anyList());
    }

    @Test
    void postRegisterNewUser_negative_Validator() {

        webClient.post().uri("/register")
                .header(HttpHeaders.AUTHORIZATION, "Basic " + credentials(INVITATION, true))
                .body(Mono.just(new PostRegisterNewUserRequest("", "test-password")), PostRegisterNewUserRequest.class)
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentType(MediaType.TEXT_PLAIN)
                .expectBody(String.class).isEqualTo("Email is required");

        verify(userDetailsService, times(1)).findByUsername(any(String.class));
        verify(userDetailsService, times(0)).createUser(any(String.class), any(String.class), ArgumentMatchers.<String>anyList());
    }

    @Test
    void postRegisterNewUser_negative_InternalServerError() {

        doReturn(Mono.error(new RuntimeException("Bad"))).when(userDetailsService).createUser(any(String.class), any(String.class), ArgumentMatchers.<String>anyList());

        webClient.post().uri("/register")
                .header(HttpHeaders.AUTHORIZATION, "Basic " + credentials(INVITATION, true))
                .body(Mono.just(new PostRegisterNewUserRequest("test-user@mail.com", "test-password")), PostRegisterNewUserRequest.class)
                .exchange()
                .expectStatus().is5xxServerError()
                .expectBody().isEmpty();

        verify(userDetailsService, times(1)).findByUsername(any(String.class));
        verify(userDetailsService, times(1)).createUser(any(String.class), any(String.class), ArgumentMatchers.<String>anyList());
    }

    @Test
    void postRegisterNewUser_negative_ResponseStatusException() {

        doReturn(Mono.error(new ResponseStatusException(HttpStatus.I_AM_A_TEAPOT, "Bad"))).when(userDetailsService).createUser(any(String.class), any(String.class), ArgumentMatchers.<String>anyList());

        webClient.post().uri("/register")
                .header(HttpHeaders.AUTHORIZATION, "Basic " + credentials(INVITATION, true))
                .body(Mono.just(new PostRegisterNewUserRequest("test-user@mail.com", "test-password")), PostRegisterNewUserRequest.class)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.I_AM_A_TEAPOT)
                .expectBody(String.class).isEqualTo("Bad");

        verify(userDetailsService, times(1)).findByUsername(any(String.class));
        verify(userDetailsService, times(1)).createUser(any(String.class), any(String.class), ArgumentMatchers.<String>anyList());
    }

    @Test
    void postChangePassword_positive_User() {

        doReturn(Mono.just(new User())).when(userDetailsService).changePassword(any(User.class), any(String.class));

        webClient.post().uri("/settings/change-password")
                .header(HttpHeaders.AUTHORIZATION, "Basic " + credentials(USER, true))
                .body(Mono.just("new-password"), String.class)
                .exchange()
                .expectStatus().isOk()
                .expectBody().isEmpty();

        verify(userDetailsService, times(2)).findByUsername(USER);
        verify(userDetailsService, times(1)).changePassword(any(User.class), any(String.class));

        final ArgumentCaptor<User> argumentCaptorUser = ArgumentCaptor.forClass(User.class);
        final ArgumentCaptor<String> argumentCaptorNewPassword = ArgumentCaptor.forClass(String.class);

        verify(userDetailsService).changePassword(argumentCaptorUser.capture(), argumentCaptorNewPassword.capture());

        final User user = argumentCaptorUser.getValue();
        assertThat(user.getUsername(), is(USER));
        assertThat(user.getPassword(), is(ENCODED_PASSWORD));
        assertThat(user.getRoles(), is(USER));

        assertThat(argumentCaptorNewPassword.getValue(), is("new-password"));
    }

    @Test
    void postChangePassword_positive_Jwt() {

        doReturn(Mono.just(new User())).when(userDetailsService).changePassword(any(User.class), any(String.class));

        webClient.post().uri("/settings/change-password")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + testJwt(USER))
                .body(Mono.just("new-password"), String.class)
                .exchange()
                .expectStatus().isOk()
                .expectBody().isEmpty();

        verify(userDetailsService, times(1)).findByUsername(USER);
        verify(userDetailsService, times(1)).changePassword(any(User.class), any(String.class));

        final ArgumentCaptor<User> argumentCaptorUser = ArgumentCaptor.forClass(User.class);
        final ArgumentCaptor<String> argumentCaptorNewPassword = ArgumentCaptor.forClass(String.class);

        verify(userDetailsService).changePassword(argumentCaptorUser.capture(), argumentCaptorNewPassword.capture());

        final User user = argumentCaptorUser.getValue();
        assertThat(user.getUsername(), is(USER));
        assertThat(user.getPassword(), is(ENCODED_PASSWORD));
        assertThat(user.getRoles(), is(USER));

        assertThat(argumentCaptorNewPassword.getValue(), is("new-password"));
    }

    @Test
    void postChangePassword_negative_NoBody() {

        webClient.post().uri("/settings/change-password")
                .header(HttpHeaders.AUTHORIZATION, "Basic " + credentials(USER, true))
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentType(MediaType.TEXT_PLAIN)
                .expectBody(String.class).isEqualTo("Body required");

        verify(userDetailsService, times(2)).findByUsername(USER);
        verify(userDetailsService, times(0)).changePassword(any(User.class), any(String.class));
    }

    @Test
    void postChangePassword_negative_InternalServerError() {

        doReturn(Mono.error(new RuntimeException("Bad"))).when(userDetailsService).changePassword(any(User.class), any(String.class));

        webClient.post().uri("/settings/change-password")
                .header(HttpHeaders.AUTHORIZATION, "Basic " + credentials(USER, true))
                .body(Mono.just("new-password"), String.class)
                .exchange()
                .expectStatus().is5xxServerError()
                .expectBody().isEmpty();

        verify(userDetailsService, times(2)).findByUsername(USER);
        verify(userDetailsService, times(1)).changePassword(any(User.class), any(String.class));
    }

    @Test
    void postChangeInvitationCode_positive_User() {

        doReturn(Mono.just(new User())).when(userDetailsService).changePassword(any(User.class), any(String.class));

        webClient.post().uri("/settings/change-invitation-code")
                .header(HttpHeaders.AUTHORIZATION, "Basic " + credentials(ADMIN, true))
                .body(Mono.just("new-invitation-code"), String.class)
                .exchange()
                .expectStatus().isOk()
                .expectBody().isEmpty();

        verify(userDetailsService, times(1)).findByUsername(ADMIN);
        verify(userDetailsService, times(1)).findByUsername("invitation-code");
        verify(userDetailsService, times(1)).changePassword(any(User.class), any(String.class));

        final ArgumentCaptor<User> argumentCaptorUser = ArgumentCaptor.forClass(User.class);
        final ArgumentCaptor<String> argumentCaptorNewPassword = ArgumentCaptor.forClass(String.class);

        verify(userDetailsService).changePassword(argumentCaptorUser.capture(), argumentCaptorNewPassword.capture());

        final User user = argumentCaptorUser.getValue();
        assertThat(user.getUsername(), is("invitation-code"));
        assertThat(user.getPassword(), is(ENCODED_PASSWORD));
        assertThat(user.getRoles(), is(INVITATION));

        assertThat(argumentCaptorNewPassword.getValue(), is("new-invitation-code"));
    }

    @Test
    void postChangeInvitationCode_positive_Jwt() {

        doReturn(Mono.just(new User())).when(userDetailsService).changePassword(any(User.class), any(String.class));

        webClient.post().uri("/settings/change-invitation-code")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + testJwt(ADMIN))
                .body(Mono.just("new-invitation-code"), String.class)
                .exchange()
                .expectStatus().isOk()
                .expectBody().isEmpty();

        verify(userDetailsService, times(1)).findByUsername("invitation-code");
        verify(userDetailsService, times(1)).changePassword(any(User.class), any(String.class));

        final ArgumentCaptor<User> argumentCaptorUser = ArgumentCaptor.forClass(User.class);
        final ArgumentCaptor<String> argumentCaptorNewPassword = ArgumentCaptor.forClass(String.class);

        verify(userDetailsService).changePassword(argumentCaptorUser.capture(), argumentCaptorNewPassword.capture());

        final User user = argumentCaptorUser.getValue();
        assertThat(user.getUsername(), is("invitation-code"));
        assertThat(user.getPassword(), is(ENCODED_PASSWORD));
        assertThat(user.getRoles(), is(INVITATION));

        assertThat(argumentCaptorNewPassword.getValue(), is("new-invitation-code"));
    }

    @Test
    void postChangeInvitationCode_positive_NoBody() {

        webClient.post().uri("/settings/change-invitation-code")
                .header(HttpHeaders.AUTHORIZATION, "Basic " + credentials(ADMIN, true))
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentType(MediaType.TEXT_PLAIN)
                .expectBody(String.class).isEqualTo("Body required");

        verify(userDetailsService, times(1)).findByUsername(ADMIN);
        verify(userDetailsService, times(0)).changePassword(any(User.class), any(String.class));
    }

    @Test
    void postChangeInvitationCode_negative_InternalServerError() {

        doReturn(Mono.error(new RuntimeException("Bad"))).when(userDetailsService).changePassword(any(User.class), any(String.class));

        webClient.post().uri("/settings/change-invitation-code")
                .header(HttpHeaders.AUTHORIZATION, "Basic " + credentials(ADMIN, true))
                .body(Mono.just("invitation-code"), String.class)
                .exchange()
                .expectStatus().is5xxServerError()
                .expectBody().isEmpty();

        verify(userDetailsService, times(1)).findByUsername(ADMIN);
        verify(userDetailsService, times(1)).findByUsername("invitation-code");
        verify(userDetailsService, times(1)).changePassword(any(User.class), any(String.class));
    }

    @Test
    void postCreateProduct_positive_User() {

        doReturn(Mono.empty()).when(productService).createProduct(any(String.class), any(BigDecimal.class));

        webClient.post().uri("/products/create-product")
                .header(HttpHeaders.AUTHORIZATION, "Basic " + credentials(ADMIN, true))
                .body(Mono.just(new PostCreateProductRequest("test-name", BigDecimal.ONE)), PostCreateProductRequest.class)
                .exchange()
                .expectStatus().isOk()
                .expectBody().isEmpty();

        verify(productService, times(1)).createProduct("test-name", BigDecimal.ONE);
    }

    @Test
    void postCreateProduct_positive_Jwt() {

        doReturn(Mono.empty()).when(productService).createProduct(any(String.class), any(BigDecimal.class));

        webClient.post().uri("/products/create-product")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + testJwt(ADMIN))
                .body(Mono.just(new PostCreateProductRequest("test-name", BigDecimal.ONE)), PostCreateProductRequest.class)
                .exchange()
                .expectStatus().isOk()
                .expectBody().isEmpty();

        verify(productService, times(1)).createProduct("test-name", BigDecimal.ONE);
    }

    @Test
    void postCreateProduct_negative_NoBody() {

        doReturn(Mono.empty()).when(productService).createProduct(any(String.class), any(BigDecimal.class));

        webClient.post().uri("/products/create-product")
                .header(HttpHeaders.AUTHORIZATION, "Basic " + credentials(ADMIN, true))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(String.class).isEqualTo("Body required");

        verify(productService, times(0)).createProduct(any(String.class), any(BigDecimal.class));
    }

    @Test
    void postCreateProduct_negative_BodyWrongType() {

        doReturn(Mono.empty()).when(productService).createProduct(any(String.class), any(BigDecimal.class));

        webClient.post().uri("/products/create-product")
                .header(HttpHeaders.AUTHORIZATION, "Basic " + credentials(ADMIN, true))
                .body(Mono.just("Wrong"), String.class)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .expectHeader().contentType(MediaType.TEXT_PLAIN)
                .expectBody(String.class).value(s -> stringContainsInOrder("Content type '", "' not supported. Supported: "));

        verify(productService, times(0)).createProduct(any(String.class), any(BigDecimal.class));
    }

    @Test
    void postCreateProduct_negative_Validator() {

        doReturn(Mono.empty()).when(productService).createProduct(any(String.class), any(BigDecimal.class));

        webClient.post().uri("/products/create-product")
                .header(HttpHeaders.AUTHORIZATION, "Basic " + credentials(ADMIN, true))
                .body(Mono.just(new PostCreateProductRequest(null, BigDecimal.ONE)), PostCreateProductRequest.class)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(String.class).isEqualTo("Product name is required");

        verify(productService, times(0)).createProduct(any(String.class), any(BigDecimal.class));
    }

    @Test
    void postCreateProduct_negative_InternalServerError() {

        doReturn(Mono.error(new RuntimeException("Bad"))).when(productService).createProduct(any(String.class), any(BigDecimal.class));

        webClient.post().uri("/products/create-product")
                .header(HttpHeaders.AUTHORIZATION, "Basic " + credentials(ADMIN, true))
                .body(Mono.just(new PostCreateProductRequest("test-name", BigDecimal.ONE)), PostCreateProductRequest.class)
                .exchange()
                .expectStatus().is5xxServerError()
                .expectBody().isEmpty();

        verify(productService, times(1)).createProduct("test-name", BigDecimal.ONE);
    }

    @Test
    void postReadProduct_positive_User() {

        doReturn(Mono.just(
                new GetProductsResponse(2L, "name-1", BigDecimal.ONE)
        )).when(productService).readProduct(any(Long.class));

        webClient.post().uri("/products/read-product")
                .header(HttpHeaders.AUTHORIZATION, "Basic " + credentials(ADMIN, true))
                .body(Mono.just(new PostReadProductRequest(2L)), PostReadProductRequest.class)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody(GetProductsResponse.class).isEqualTo(
                        new GetProductsResponse(2L, "name-1", BigDecimal.ONE)
                );

        verify(productService, times(1)).readProduct(2L);
    }

    @Test
    void postReadProduct_positive_Jwt() {

        doReturn(Mono.just(
                new GetProductsResponse(2L, "name-1", BigDecimal.ONE)
        )).when(productService).readProduct(any(Long.class));

        webClient.post().uri("/products/read-product")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + testJwt(ADMIN, List.of(USER, ADMIN)))
                .body(Mono.just(new PostReadProductRequest(2L)), PostReadProductRequest.class)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody(GetProductsResponse.class).isEqualTo(
                        new GetProductsResponse(2L, "name-1", BigDecimal.ONE)
                );

        verify(productService, times(1)).readProduct(2L);
    }

    @Test
    void postReadProduct_negative_NoBody() {

        doReturn(Mono.just(
                new GetProductsResponse(2L, "name-1", BigDecimal.ONE)
        )).when(productService).readProduct(any(Long.class));

        webClient.post().uri("/products/read-product")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + testJwt(ADMIN, List.of(USER, ADMIN)))
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentType(MediaType.TEXT_PLAIN)
                .expectBody(String.class).isEqualTo("Body required");

        verify(productService, times(0)).readProduct(any(Long.class));
    }

    @Test
    void postReadProduct_negative_BodyWrongType() {

        doReturn(Mono.just(
                new GetProductsResponse(2L, "name-1", BigDecimal.ONE)
        )).when(productService).readProduct(any(Long.class));

        webClient.post().uri("/products/read-product")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + testJwt(ADMIN, List.of(USER, ADMIN)))
                .body(Mono.just("Wrong"), String.class)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .expectHeader().contentType(MediaType.TEXT_PLAIN)
                .expectBody(String.class).value(s -> stringContainsInOrder("Content type '", "' not supported. Supported: "));

        verify(productService, times(0)).readProduct(any(Long.class));
    }

    @Test
    void postReadProduct_negative_Validator() {

        doReturn(Mono.just(
                new GetProductsResponse(2L, "name-1", BigDecimal.ONE)
        )).when(productService).readProduct(any(Long.class));

        webClient.post().uri("/products/read-product")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + testJwt(ADMIN, List.of(USER, ADMIN)))
                .body(Mono.just(new PostReadProductRequest(null)), PostReadProductRequest.class)
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentType(MediaType.TEXT_PLAIN)
                .expectBody(String.class).isEqualTo("Product ID is required");

        verify(productService, times(0)).readProduct(any(Long.class));
    }

    @Test
    void postReadProduct_negative_InternalServerError() {

        doReturn(Mono.error(new RuntimeException("Bad"))).when(productService).readProduct(any(Long.class));

        webClient.post().uri("/products/read-product")
                .header(HttpHeaders.AUTHORIZATION, "Basic " + credentials(ADMIN, true))
                .body(Mono.just(new PostReadProductRequest(2L)), PostReadProductRequest.class)
                .exchange()
                .expectStatus().is5xxServerError()
                .expectBody().isEmpty();


        verify(productService, times(1)).readProduct(2L);
    }

    @Test
    void getReadProducts_positive_User() {

        doReturn(Mono.just(List.of(
                new GetProductsResponse(2L, "name-1", BigDecimal.ONE),
                new GetProductsResponse(1L, "name-2", BigDecimal.TWO)
        ))).when(productService).readProducts();

        webClient.get().uri("/products")
                .header(HttpHeaders.AUTHORIZATION, "Basic " + credentials(ADMIN, true))
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBodyList(GetProductsResponse.class).isEqualTo(List.of(
                        new GetProductsResponse(2L, "name-1", BigDecimal.ONE),
                        new GetProductsResponse(1L, "name-2", BigDecimal.TWO)
                ));

        verify(productService, times(1)).readProducts();
    }

    @Test
    void getReadProducts_positive_Jwt() {

        doReturn(Mono.just(List.of(
                new GetProductsResponse(2L, "name-1", BigDecimal.ONE),
                new GetProductsResponse(1L, "name-2", BigDecimal.TWO)
        ))).when(productService).readProducts();

        webClient.get().uri("/products")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + testJwt(ADMIN, List.of(USER, ADMIN)))
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBodyList(GetProductsResponse.class).isEqualTo(List.of(
                        new GetProductsResponse(2L, "name-1", BigDecimal.ONE),
                        new GetProductsResponse(1L, "name-2", BigDecimal.TWO)
                ));

        verify(productService, times(1)).readProducts();
    }

    @Test
    void getReadProducts_negative_InternalServerError() {

        doReturn(Mono.error(new RuntimeException("Bad"))).when(productService).readProducts();

        webClient.get().uri("/products")
                .header(HttpHeaders.AUTHORIZATION, "Basic " + credentials(ADMIN, true))
                .exchange()
                .expectStatus().is5xxServerError()
                .expectBody().isEmpty();


        verify(productService, times(1)).readProducts();
    }

    @Test
    void postUpdateProduct_positive_User() {

        doReturn(Mono.empty()).when(productService).updateProduct(any(Long.class), any(String.class));

        webClient.post().uri("/products/update-product")
                .header(HttpHeaders.AUTHORIZATION, "Basic " + credentials(ADMIN, true))
                .body(Mono.just(new PostUpdateProductRequest(1L, "new-name")), PostUpdateProductRequest.class)
                .exchange()
                .expectStatus().isOk()
                .expectBody().isEmpty();

        verify(productService, times(1)).updateProduct(1L, "new-name");
    }

    @Test
    void postUpdateProduct_positive_Jwt() {

        doReturn(Mono.empty()).when(productService).updateProduct(any(Long.class), any(String.class));

        webClient.post().uri("/products/update-product")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + testJwt(ADMIN))
                .body(Mono.just(new PostUpdateProductRequest(1L, "new-name")), PostUpdateProductRequest.class)
                .exchange()
                .expectStatus().isOk()
                .expectBody().isEmpty();

        verify(productService, times(1)).updateProduct(1L, "new-name");
    }

    @Test
    void postUpdateProduct_negative_NoBody() {

        doReturn(Mono.empty()).when(productService).updateProduct(any(Long.class), any(String.class));

        webClient.post().uri("/products/update-product")
                .header(HttpHeaders.AUTHORIZATION, "Basic " + credentials(ADMIN, true))
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentType(MediaType.TEXT_PLAIN)
                .expectBody(String.class).isEqualTo("Body required");

        verify(productService, times(0)).updateProduct(any(Long.class), any(String.class));
    }

    @Test
    void postUpdateProduct_negative_BodyWrongType() {

        doReturn(Mono.empty()).when(productService).updateProduct(any(Long.class), any(String.class));

        webClient.post().uri("/products/update-product")
                .header(HttpHeaders.AUTHORIZATION, "Basic " + credentials(ADMIN, true))
                .body(Mono.just("Wrong"), String.class)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .expectHeader().contentType(MediaType.TEXT_PLAIN)
                .expectBody(String.class).value(s -> stringContainsInOrder("Content type '", "' not supported. Supported: "));

        verify(productService, times(0)).updateProduct(any(Long.class), any(String.class));
    }

    @Test
    void postUpdateProduct_negative_Validator() {

        doReturn(Mono.empty()).when(productService).updateProduct(any(Long.class), any(String.class));

        webClient.post().uri("/products/update-product")
                .header(HttpHeaders.AUTHORIZATION, "Basic " + credentials(ADMIN, true))
                .body(Mono.just(new PostUpdateProductRequest(null, "new-name")), PostUpdateProductRequest.class)
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentType(MediaType.TEXT_PLAIN)
                .expectBody(String.class).isEqualTo("Product ID is required");

        verify(productService, times(0)).updateProduct(any(Long.class), any(String.class));
    }

    @Test
    void postUpdateProduct_negative_InternalServerError() {

        doReturn(Mono.error(new RuntimeException("Bad"))).when(productService).updateProduct(any(Long.class), any(String.class));

        webClient.post().uri("/products/update-product")
                .header(HttpHeaders.AUTHORIZATION, "Basic " + credentials(ADMIN, true))
                .body(Mono.just(new PostUpdateProductRequest(1L, "new-name")), PostUpdateProductRequest.class)
                .exchange()
                .expectStatus().is5xxServerError()
                .expectBody().isEmpty();

        verify(productService, times(1)).updateProduct(1L, "new-name");
    }

    @Test
    void postUpdateProductPrice_positive_User() {

        doReturn(Mono.empty()).when(productService).updateProductPrice(any(Long.class), any(BigDecimal.class));

        webClient.post().uri("/products/update-price")
                .header(HttpHeaders.AUTHORIZATION, "Basic " + credentials(ADMIN, true))
                .body(Mono.just(new PostUpdateProductPriceRequest(1L, BigDecimal.ONE)), PostUpdateProductRequest.class)
                .exchange()
                .expectStatus().isOk()
                .expectBody().isEmpty();

        verify(productService, times(1)).updateProductPrice(1L, BigDecimal.ONE);
    }

    @Test
    void postUpdateProductPrice_positive_Jwt() {

        doReturn(Mono.empty()).when(productService).updateProductPrice(any(Long.class), any(BigDecimal.class));

        webClient.post().uri("/products/update-price")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + testJwt(ADMIN))
                .body(Mono.just(new PostUpdateProductPriceRequest(1L, BigDecimal.ONE)), PostUpdateProductRequest.class)
                .exchange()
                .expectStatus().isOk()
                .expectBody().isEmpty();

        verify(productService, times(1)).updateProductPrice(1L, BigDecimal.ONE);
    }

    @Test
    void postUpdateProductPrice_negative_NoBody() {

        doReturn(Mono.empty()).when(productService).updateProductPrice(any(Long.class), any(BigDecimal.class));

        webClient.post().uri("/products/update-price")
                .header(HttpHeaders.AUTHORIZATION, "Basic " + credentials(ADMIN, true))
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentType(MediaType.TEXT_PLAIN)
                .expectBody(String.class).isEqualTo("Body required");

        verify(productService, times(0)).updateProductPrice(any(Long.class), any(BigDecimal.class));
    }

    @Test
    void postUpdateProductPrice_negative_BodyWrongType() {

        doReturn(Mono.empty()).when(productService).updateProductPrice(any(Long.class), any(BigDecimal.class));

        webClient.post().uri("/products/update-price")
                .header(HttpHeaders.AUTHORIZATION, "Basic " + credentials(ADMIN, true))
                .body(Mono.just("Wrong"), String.class)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .expectHeader().contentType(MediaType.TEXT_PLAIN)
                .expectBody(String.class).value(s -> stringContainsInOrder("Content type '", "' not supported. Supported: "));

        verify(productService, times(0)).updateProductPrice(any(Long.class), any(BigDecimal.class));
    }

    @Test
    void postUpdateProductPrice_negative_Validator() {

        doReturn(Mono.empty()).when(productService).updateProductPrice(any(Long.class), any(BigDecimal.class));

        webClient.post().uri("/products/update-price")
                .header(HttpHeaders.AUTHORIZATION, "Basic " + credentials(ADMIN, true))
                .body(Mono.just(new PostUpdateProductPriceRequest(null, BigDecimal.ONE)), PostUpdateProductRequest.class)
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentType(MediaType.TEXT_PLAIN)
                .expectBody(String.class).isEqualTo("Product ID is required");

        verify(productService, times(0)).updateProductPrice(any(Long.class), any(BigDecimal.class));
    }

    @Test
    void postUpdateProductPrice_negative_InternalServerError() {

        doReturn(Mono.error(new RuntimeException("Bad"))).when(productService).updateProductPrice(any(Long.class), any(BigDecimal.class));

        webClient.post().uri("/products/update-price")
                .header(HttpHeaders.AUTHORIZATION, "Basic " + credentials(ADMIN, true))
                .body(Mono.just(new PostUpdateProductPriceRequest(1L, BigDecimal.ONE)), PostUpdateProductRequest.class)
                .exchange()
                .expectStatus().is5xxServerError()
                .expectBody().isEmpty();

        verify(productService, times(1)).updateProductPrice(1L, BigDecimal.ONE);
    }

    @Test
    void postCreatePurchase_positive_User() {

        doReturn(Mono.empty()).when(purchaseService).createPurchase(any(Long.class), any(Long.class));

        webClient.post().uri("/purchases/create-purchase")
                .header(HttpHeaders.AUTHORIZATION, "Basic " + credentials(USER, true))
                .body(Mono.just(new PostCreatePurchaseRequest(1L)), PostCreatePurchaseRequest.class)
                .exchange()
                .expectStatus().isOk()
                .expectBody().isEmpty();

        verify(purchaseService, times(1)).createPurchase(2L, 1L);
    }

    @Test
    void postCreatePurchase_positive_Jwt() {

        doReturn(Mono.empty()).when(purchaseService).createPurchase(any(Long.class), any(Long.class));

        webClient.post().uri("/purchases/create-purchase")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + testJwt(USER))
                .body(Mono.just(new PostCreatePurchaseRequest(1L)), PostCreatePurchaseRequest.class)
                .exchange()
                .expectStatus().isOk()
                .expectBody().isEmpty();

        verify(purchaseService, times(1)).createPurchase(2L, 1L);
    }

    @Test
    void postCreatePurchase_negative_NoBody() {

        doReturn(Mono.empty()).when(purchaseService).createPurchase(any(Long.class), any(Long.class));

        webClient.post().uri("/purchases/create-purchase")
                .header(HttpHeaders.AUTHORIZATION, "Basic " + credentials(USER, true))
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentType(MediaType.TEXT_PLAIN)
                .expectBody(String.class).isEqualTo("Body required");

        verify(purchaseService, times(0)).createPurchase(any(Long.class), any(Long.class));
    }

    @Test
    void postCreatePurchase_negative_ProductIdWrongType() {

        doReturn(Mono.empty()).when(purchaseService).createPurchase(any(Long.class), any(Long.class));

        webClient.post().uri("/purchases/create-purchase")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + testJwt(USER))
                .body(Mono.just("Wrong"), String.class)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .expectHeader().contentType(MediaType.TEXT_PLAIN)
                .expectBody(String.class).value(s -> stringContainsInOrder("Content type '", "' not supported. Supported: "));

        verify(purchaseService, times(0)).createPurchase(any(Long.class), any(Long.class));
    }

    @Test
    void postCreatePurchase_negative_Validator() {

        doReturn(Mono.empty()).when(purchaseService).createPurchase(any(Long.class), any(Long.class));

        webClient.post().uri("/purchases/create-purchase")
                .header(HttpHeaders.AUTHORIZATION, "Basic " + credentials(USER, true))
                .body(Mono.just(new PostCreatePurchaseRequest(null)), PostCreatePurchaseRequest.class)
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentType(MediaType.TEXT_PLAIN)
                .expectBody(String.class).isEqualTo("Product ID is required");


        verify(purchaseService, times(0)).createPurchase(any(Long.class), any(Long.class));
    }

    @Test
    void postCreatePurchase_negative_InternalServerError() {

        doReturn(Mono.error(new RuntimeException("Bad"))).when(purchaseService).createPurchase(any(Long.class), any(Long.class));

        webClient.post().uri("/purchases/create-purchase")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + testJwt(USER))
                .body(Mono.just(new PostCreatePurchaseRequest(1L)), PostCreatePurchaseRequest.class)
                .exchange()
                .expectStatus().is5xxServerError()
                .expectBody().isEmpty();

        verify(purchaseService, times(1)).createPurchase(any(Long.class), any(Long.class));
    }

    @Test
    void getReadPurchases_positive_User() {

        doReturn(Mono.just(List.of(
                new GetPurchasesResponse(1L, LocalDateTime.of(2024, 5, 1, 12, 54, 12), "product", BigDecimal.ONE)
        ))).when(purchaseService).readPurchases(any(Long.class));

        webClient.get().uri("/purchases")
                .header(HttpHeaders.AUTHORIZATION, "Basic " + credentials(USER, true))
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBodyList(GetPurchasesResponse.class).isEqualTo(List.of(
                        new GetPurchasesResponse(1L, LocalDateTime.of(2024, 5, 1, 12, 54, 12), "product", BigDecimal.ONE)
                ));

        verify(purchaseService, times(1)).readPurchases(2L);
    }

    @Test
    void getReadPurchases_positive_Jwt() {

        doReturn(Mono.just(List.of(
                new GetPurchasesResponse(1L, LocalDateTime.of(2024, 5, 1, 12, 54, 12), "product", BigDecimal.ONE)
        ))).when(purchaseService).readPurchases(any(Long.class));

        webClient.get().uri("/purchases")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + testJwt(USER))
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBodyList(GetPurchasesResponse.class).isEqualTo(List.of(
                        new GetPurchasesResponse(1L, LocalDateTime.of(2024, 5, 1, 12, 54, 12), "product", BigDecimal.ONE)
                ));

        verify(purchaseService, times(1)).readPurchases(2L);
    }

    @Test
    void getReadPurchases_negative_InternalServerError() {

        doReturn(Mono.error(new RuntimeException("Bad"))).when(purchaseService).readPurchases(any(Long.class));

        webClient.get().uri("/purchases")
                .header(HttpHeaders.AUTHORIZATION, "Basic " + credentials(USER, true))
                .exchange()
                .expectStatus().is5xxServerError()
                .expectBody().isEmpty();

        verify(purchaseService, times(1)).readPurchases(any(Long.class));
    }

    @Test
    void postDeletePurchase_positive_User() {

        doReturn(Mono.empty()).when(purchaseService).deletePurchase(any(Long.class));

        webClient.post().uri("/purchases/delete-purchase")
                .header(HttpHeaders.AUTHORIZATION, "Basic " + credentials(USER, true))
                .body(Mono.just(new PostDeletePurchaseRequest(1L)), PostDeletePurchaseRequest.class)
                .exchange()
                .expectStatus().isOk()
                .expectBody().isEmpty();

        verify(purchaseService, times(1)).deletePurchase(1L);
    }

    @Test
    void postDeletePurchase_positive_Jwt() {

        doReturn(Mono.empty()).when(purchaseService).deletePurchase(any(Long.class));

        webClient.post().uri("/purchases/delete-purchase")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + testJwt(USER))
                .body(Mono.just(new PostDeletePurchaseRequest(1L)), PostDeletePurchaseRequest.class)
                .exchange()
                .expectStatus().isOk()
                .expectBody().isEmpty();

        verify(purchaseService, times(1)).deletePurchase(1L);
    }

    @Test
    void postDeletePurchase_negative_NoBody() {

        doReturn(Mono.empty()).when(purchaseService).deletePurchase(any(Long.class));

        webClient.post().uri("/purchases/delete-purchase")
                .header(HttpHeaders.AUTHORIZATION, "Basic " + credentials(USER, true))
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentType(MediaType.TEXT_PLAIN)
                .expectBody(String.class).isEqualTo("Body required");

        verify(purchaseService, times(0)).deletePurchase(any(Long.class));
    }

    @Test
    void postDeletePurchase_negative_PurchaseIdWrongType() {

        doReturn(Mono.empty()).when(purchaseService).deletePurchase(any(Long.class));

        webClient.post().uri("/purchases/delete-purchase")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + testJwt(USER))
                .body(Mono.just("Wrong"), String.class)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .expectHeader().contentType(MediaType.TEXT_PLAIN)
                .expectBody(String.class).value(s -> stringContainsInOrder("Content type '", "' not supported. Supported: "));

        verify(purchaseService, times(0)).deletePurchase(any(Long.class));
    }

    @Test
    void postDeletePurchase_negative_Validator() {

        doReturn(Mono.empty()).when(purchaseService).deletePurchase(any(Long.class));

        webClient.post().uri("/purchases/delete-purchase")
                .header(HttpHeaders.AUTHORIZATION, "Basic " + credentials(USER, true))
                .body(Mono.just(new PostDeletePurchaseRequest(null)), PostDeletePurchaseRequest.class)
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentType(MediaType.TEXT_PLAIN)
                .expectBody(String.class).isEqualTo("Purchase ID is required");


        verify(purchaseService, times(0)).deletePurchase(any(Long.class));
    }

    @Test
    void postDeletePurchase_negative_InternalServerError() {

        doReturn(Mono.error(new RuntimeException("Bad"))).when(purchaseService).deletePurchase(any(Long.class));

        webClient.post().uri("/purchases/delete-purchase")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + testJwt(USER))
                .body(Mono.just(new PostDeletePurchaseRequest(1L)), PostDeletePurchaseRequest.class)
                .exchange()
                .expectStatus().is5xxServerError()
                .expectBody().isEmpty();

        verify(purchaseService, times(1)).deletePurchase(any(Long.class));
    }
}

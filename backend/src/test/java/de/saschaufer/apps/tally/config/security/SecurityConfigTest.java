package de.saschaufer.apps.tally.config.security;

import de.saschaufer.apps.tally.controller.Handler;
import de.saschaufer.apps.tally.controller.dto.PostLoginResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.server.ServerRequest;

import java.time.Instant;

import static de.saschaufer.apps.tally.persistence.dto.User.Role.*;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

class SecurityConfigTest extends SecurityConfigSetup {

    @MockBean
    Handler handler;

    @Test
    void root_positive_IndexHtml() {

        webClient.get().uri("/")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(startsWith("<!doctype html>"));
    }

    @Test
    void getIndexHtml_negative_HtmlBlocked() {

        webClient.get().uri("/index.html")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody().isEmpty();
    }

    @Test
    void getJs_positive() {

        webClient.get().uri("/script.js")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(startsWith("code"));
    }

    @Test
    void getCss_positive() {

        webClient.get().uri("/styles.css")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(startsWith("style"));
    }

    @Test
    void getIco_positive() {

        webClient.get().uri("/icon.ico")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(startsWith("icon"));
    }

    @Test
    void postLogin_positive_Password() {

        doReturn(ok().bodyValue(new PostLoginResponse("jwt", true))).when(handler).postLogin(any(ServerRequest.class));

        webClient.post().uri("/login")
                .header(HttpHeaders.AUTHORIZATION, "Basic " + credentials(USER, true))
                .exchange()
                .expectStatus().isOk()
                .expectBody(PostLoginResponse.class).isEqualTo(new PostLoginResponse("jwt", true));

        verify(handler, times(1)).postLogin(any(ServerRequest.class));
    }

    @Test
    void postLogin_negative_UserWrong() {

        webClient.post().uri("/login")
                .header(HttpHeaders.AUTHORIZATION, "Basic " + credentials(USER + "-wrong", false))
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody().isEmpty();

        verify(handler, times(0)).postLogin(any(ServerRequest.class));
    }

    @Test
    void postLogin_negative_PasswordWrong() {

        webClient.post().uri("/login")
                .header(HttpHeaders.AUTHORIZATION, "Basic " + credentials(USER, false))
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody().isEmpty();

        verify(handler, times(0)).postLogin(any(ServerRequest.class));
    }

    @Test
    void postLogin_positive_Jwt() {

        doReturn(ok().bodyValue(new PostLoginResponse("jwt", true))).when(handler).postLogin(any(ServerRequest.class));

        webClient.post().uri("/login")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + testJwt(USER, Instant.now()))
                .exchange()
                .expectStatus().isOk()
                .expectBody(PostLoginResponse.class).isEqualTo(new PostLoginResponse("jwt", true));

        verify(handler, times(1)).postLogin(any(ServerRequest.class));
    }

    @Test
    void postLogin_negative_JwtExpired() {

        webClient.post().uri("/login")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + testJwt(USER, Instant.now().minusSeconds(100000)))
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody().isEmpty();

        verify(handler, times(0)).postLogin(any(ServerRequest.class));
    }

    @Test
    void postRegister_positive_Password() {

        doReturn(ok().build()).when(handler).postRegisterNewUser(any(ServerRequest.class));

        webClient.post().uri("/register")
                .header(HttpHeaders.AUTHORIZATION, "Basic " + credentials(INVITATION, true))
                .exchange()
                .expectStatus().isOk()
                .expectBody().isEmpty();

        verify(handler, times(1)).postRegisterNewUser(any(ServerRequest.class));
    }

    @Test
    void postRegister_negative_PasswordUserWrongRole() {

        webClient.post().uri("/register")
                .header(HttpHeaders.AUTHORIZATION, "Basic " + credentials(USER, true))
                .exchange()
                .expectStatus().isForbidden()
                .expectBody().isEmpty();

        verify(handler, times(0)).postRegisterNewUser(any(ServerRequest.class));
    }

    @Test
    void postRegister_positive_Jwt() {

        doReturn(ok().build()).when(handler).postRegisterNewUser(any(ServerRequest.class));

        webClient.post().uri("/register")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + testJwt(INVITATION, Instant.now()))
                .exchange()
                .expectStatus().isOk()
                .expectBody().isEmpty();

        verify(handler, times(1)).postRegisterNewUser(any(ServerRequest.class));
    }

    @Test
    void postRegister_negative_JwtUserWrongRole() {

        webClient.post().uri("/register")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + testJwt(USER, Instant.now()))
                .exchange()
                .expectStatus().isForbidden()
                .expectBody().isEmpty();

        verify(handler, times(0)).postRegisterNewUser(any(ServerRequest.class));
    }

    @Test
    void postRegisterNewUserConfirm_positive() {

        doReturn(ok().build()).when(handler).postRegisterNewUserConfirm(any(ServerRequest.class));

        webClient.post().uri("/register/confirm")
                .exchange()
                .expectStatus().isOk()
                .expectBody().isEmpty();

        verify(handler, times(1)).postRegisterNewUserConfirm(any(ServerRequest.class));
    }

    @Test
    void postChangePassword_positive_Password() {

        doReturn(ok().build()).when(handler).postChangePassword(any(ServerRequest.class));

        webClient.post().uri("/settings/change-password")
                .header(HttpHeaders.AUTHORIZATION, "Basic " + credentials(USER, true))
                .exchange()
                .expectStatus().isOk()
                .expectBody().isEmpty();

        verify(handler, times(1)).postChangePassword(any(ServerRequest.class));
    }

    @Test
    void postChangePassword_negative_PasswordUserWrongRole() {

        webClient.post().uri("/settings/change-password")
                .header(HttpHeaders.AUTHORIZATION, "Basic " + credentials(INVITATION, true))
                .exchange()
                .expectStatus().isForbidden()
                .expectBody().isEmpty();

        verify(handler, times(0)).postChangePassword(any(ServerRequest.class));
    }

    @Test
    void postChangePassword_positive_Jwt() {

        doReturn(ok().build()).when(handler).postChangePassword(any(ServerRequest.class));

        webClient.post().uri("/settings/change-password")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + testJwt(USER, Instant.now()))
                .exchange()
                .expectStatus().isOk()
                .expectBody().isEmpty();

        verify(handler, times(1)).postChangePassword(any(ServerRequest.class));
    }

    @Test
    void postChangePassword_negative_JwtUserWrongRole() {

        webClient.post().uri("/settings/change-password")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + testJwt(INVITATION, Instant.now()))
                .exchange()
                .expectStatus().isForbidden()
                .expectBody().isEmpty();

        verify(handler, times(0)).postChangePassword(any(ServerRequest.class));
    }

    @Test
    void postChangeInvitationCode_positive_Password() {

        doReturn(ok().build()).when(handler).postChangeInvitationCode(any(ServerRequest.class));

        webClient.post().uri("/settings/change-invitation-code")
                .header(HttpHeaders.AUTHORIZATION, "Basic " + credentials(ADMIN, true))
                .exchange()
                .expectStatus().isOk()
                .expectBody().isEmpty();

        verify(handler, times(1)).postChangeInvitationCode(any(ServerRequest.class));
    }

    @Test
    void postChangeInvitationCode_negative_PasswordUserWrongRole() {

        webClient.post().uri("/settings/change-invitation-code")
                .header(HttpHeaders.AUTHORIZATION, "Basic " + credentials(USER, true))
                .exchange()
                .expectStatus().isForbidden()
                .expectBody().isEmpty();

        verify(handler, times(0)).postChangeInvitationCode(any(ServerRequest.class));
    }

    @Test
    void postChangeInvitationCode_positive_Jwt() {

        doReturn(ok().build()).when(handler).postChangeInvitationCode(any(ServerRequest.class));

        webClient.post().uri("/settings/change-invitation-code")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + testJwt(ADMIN))
                .exchange()
                .expectStatus().isOk()
                .expectBody().isEmpty();

        verify(handler, times(1)).postChangeInvitationCode(any(ServerRequest.class));
    }

    @Test
    void postChangeInvitationCode_negative_JwtUserWrongRole() {

        webClient.post().uri("/settings/change-invitation-code")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + testJwt(USER))
                .exchange()
                .expectStatus().isForbidden()
                .expectBody().isEmpty();

        verify(handler, times(0)).postChangeInvitationCode(any(ServerRequest.class));
    }

    @Test
    void getReadProducts_positive_Password() {

        doReturn(ok().build()).when(handler).getReadProducts(any(ServerRequest.class));

        webClient.get().uri("/products")
                .header(HttpHeaders.AUTHORIZATION, "Basic " + credentials(USER, true))
                .exchange()
                .expectStatus().isOk()
                .expectBody().isEmpty();

        verify(handler, times(1)).getReadProducts(any(ServerRequest.class));
    }

    @Test
    void getReadProducts_negative_PasswordUserWrongRole() {

        webClient.get().uri("/products")
                .header(HttpHeaders.AUTHORIZATION, "Basic " + credentials(NONE, true))
                .exchange()
                .expectStatus().isForbidden()
                .expectBody().isEmpty();

        verify(handler, times(0)).getReadProducts(any(ServerRequest.class));
    }

    @Test
    void getReadProducts_positive_Jwt() {

        doReturn(ok().build()).when(handler).getReadProducts(any(ServerRequest.class));

        webClient.get().uri("/products")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + testJwt(USER))
                .exchange()
                .expectStatus().isOk()
                .expectBody().isEmpty();

        verify(handler, times(1)).getReadProducts(any(ServerRequest.class));
    }

    @Test
    void getReadProducts_negative_JwtUserWrongRole() {

        webClient.get().uri("/products")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + testJwt(NONE))
                .exchange()
                .expectStatus().isForbidden()
                .expectBody().isEmpty();

        verify(handler, times(0)).getReadProducts(any(ServerRequest.class));
    }

    @Test
    void postCreateProduct_positive_Password() {

        doReturn(ok().build()).when(handler).postCreateProduct(any(ServerRequest.class));

        webClient.post().uri("/products/create-product")
                .header(HttpHeaders.AUTHORIZATION, "Basic " + credentials(ADMIN, true))
                .exchange()
                .expectStatus().isOk()
                .expectBody().isEmpty();

        verify(handler, times(1)).postCreateProduct(any(ServerRequest.class));
    }

    @Test
    void postCreateProduct_negative_PasswordUserWrongRole() {

        webClient.post().uri("/products/create-product")
                .header(HttpHeaders.AUTHORIZATION, "Basic " + credentials(USER, true))
                .exchange()
                .expectStatus().isForbidden()
                .expectBody().isEmpty();

        verify(handler, times(0)).postCreateProduct(any(ServerRequest.class));
    }

    @Test
    void postCreateProduct_positive_Jwt() {

        doReturn(ok().build()).when(handler).postCreateProduct(any(ServerRequest.class));

        webClient.post().uri("/products/create-product")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + testJwt(ADMIN))
                .exchange()
                .expectStatus().isOk()
                .expectBody().isEmpty();

        verify(handler, times(1)).postCreateProduct(any(ServerRequest.class));
    }

    @Test
    void postCreateProduct_negative_JwtUserWrongRole() {

        webClient.post().uri("/products/create-product")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + testJwt(USER))
                .exchange()
                .expectStatus().isForbidden()
                .expectBody().isEmpty();

        verify(handler, times(0)).postCreateProduct(any(ServerRequest.class));
    }

    @Test
    void postUpdateProduct_positive_Password() {

        doReturn(ok().build()).when(handler).postUpdateProduct(any(ServerRequest.class));

        webClient.post().uri("/products/update-product")
                .header(HttpHeaders.AUTHORIZATION, "Basic " + credentials(ADMIN, true))
                .exchange()
                .expectStatus().isOk()
                .expectBody().isEmpty();

        verify(handler, times(1)).postUpdateProduct(any(ServerRequest.class));
    }

    @Test
    void postUpdateProduct_negative_PasswordUserWrongRole() {

        webClient.post().uri("/products/update-product")
                .header(HttpHeaders.AUTHORIZATION, "Basic " + credentials(USER, true))
                .exchange()
                .expectStatus().isForbidden()
                .expectBody().isEmpty();

        verify(handler, times(0)).postUpdateProduct(any(ServerRequest.class));
    }

    @Test
    void postUpdateProduct_positive_Jwt() {

        doReturn(ok().build()).when(handler).postUpdateProduct(any(ServerRequest.class));

        webClient.post().uri("/products/update-product")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + testJwt(ADMIN))
                .exchange()
                .expectStatus().isOk()
                .expectBody().isEmpty();

        verify(handler, times(1)).postUpdateProduct(any(ServerRequest.class));
    }

    @Test
    void postUpdateProduct_negative_JwtUserWrongRole() {

        webClient.post().uri("/products/update-product")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + testJwt(USER))
                .exchange()
                .expectStatus().isForbidden()
                .expectBody().isEmpty();

        verify(handler, times(0)).postUpdateProduct(any(ServerRequest.class));
    }

    @Test
    void postUpdateProductPrice_positive_Password() {

        doReturn(ok().build()).when(handler).postUpdateProductPrice(any(ServerRequest.class));

        webClient.post().uri("/products/update-price")
                .header(HttpHeaders.AUTHORIZATION, "Basic " + credentials(ADMIN, true))
                .exchange()
                .expectStatus().isOk()
                .expectBody().isEmpty();

        verify(handler, times(1)).postUpdateProductPrice(any(ServerRequest.class));
    }

    @Test
    void postUpdateProductPrice_negative_PasswordUserWrongRole() {

        webClient.post().uri("/products/update-price")
                .header(HttpHeaders.AUTHORIZATION, "Basic " + credentials(USER, true))
                .exchange()
                .expectStatus().isForbidden()
                .expectBody().isEmpty();

        verify(handler, times(0)).postUpdateProductPrice(any(ServerRequest.class));
    }

    @Test
    void postUpdateProductPrice_positive_Jwt() {

        doReturn(ok().build()).when(handler).postUpdateProductPrice(any(ServerRequest.class));

        webClient.post().uri("/products/update-price")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + testJwt(ADMIN))
                .exchange()
                .expectStatus().isOk()
                .expectBody().isEmpty();

        verify(handler, times(1)).postUpdateProductPrice(any(ServerRequest.class));
    }

    @Test
    void postUpdateProductPrice_negative_JwtUserWrongRole() {

        webClient.post().uri("/products/update-price")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + testJwt(USER))
                .exchange()
                .expectStatus().isForbidden()
                .expectBody().isEmpty();

        verify(handler, times(0)).postUpdateProductPrice(any(ServerRequest.class));
    }

    @Test
    void postCreatePurchase_positive_Password() {

        doReturn(ok().build()).when(handler).postCreatePurchase(any(ServerRequest.class));

        webClient.post().uri("/purchases/create-purchase")
                .header(HttpHeaders.AUTHORIZATION, "Basic " + credentials(USER, true))
                .exchange()
                .expectStatus().isOk()
                .expectBody().isEmpty();

        verify(handler, times(1)).postCreatePurchase(any(ServerRequest.class));
    }

    @Test
    void postCreatePurchase_negative_PasswordUserWrongRole() {

        webClient.post().uri("/purchases/create-purchase")
                .header(HttpHeaders.AUTHORIZATION, "Basic " + credentials(NONE, true))
                .exchange()
                .expectStatus().isForbidden()
                .expectBody().isEmpty();

        verify(handler, times(0)).postCreatePurchase(any(ServerRequest.class));
    }

    @Test
    void postCreatePurchase_positive_Jwt() {

        doReturn(ok().build()).when(handler).postCreatePurchase(any(ServerRequest.class));

        webClient.post().uri("/purchases/create-purchase")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + testJwt(USER))
                .exchange()
                .expectStatus().isOk()
                .expectBody().isEmpty();

        verify(handler, times(1)).postCreatePurchase(any(ServerRequest.class));
    }

    @Test
    void postCreatePurchase_negative_JwtUserWrongRole() {

        webClient.post().uri("/purchases/create-purchase")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + testJwt(NONE))
                .exchange()
                .expectStatus().isForbidden()
                .expectBody().isEmpty();

        verify(handler, times(0)).postCreatePurchase(any(ServerRequest.class));
    }

    @Test
    void getReadPurchases_positive_Password() {

        doReturn(ok().build()).when(handler).getReadPurchases(any(ServerRequest.class));

        webClient.get().uri("/purchases")
                .header(HttpHeaders.AUTHORIZATION, "Basic " + credentials(USER, true))
                .exchange()
                .expectStatus().isOk()
                .expectBody().isEmpty();

        verify(handler, times(1)).getReadPurchases(any(ServerRequest.class));
    }

    @Test
    void getReadPurchases_negative_PasswordUserWrongRole() {

        webClient.get().uri("/purchases")
                .header(HttpHeaders.AUTHORIZATION, "Basic " + credentials(NONE, true))
                .exchange()
                .expectStatus().isForbidden()
                .expectBody().isEmpty();

        verify(handler, times(0)).getReadPurchases(any(ServerRequest.class));
    }

    @Test
    void getReadPurchases_positive_Jwt() {

        doReturn(ok().build()).when(handler).getReadPurchases(any(ServerRequest.class));

        webClient.get().uri("/purchases")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + testJwt(USER))
                .exchange()
                .expectStatus().isOk()
                .expectBody().isEmpty();

        verify(handler, times(1)).getReadPurchases(any(ServerRequest.class));
    }

    @Test
    void getReadPurchases_negative_JwtUserWrongRole() {

        webClient.get().uri("/purchases")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + testJwt(NONE))
                .exchange()
                .expectStatus().isForbidden()
                .expectBody().isEmpty();

        verify(handler, times(0)).getReadPurchases(any(ServerRequest.class));
    }

    @Test
    void postDeletePurchase_positive_Password() {

        doReturn(ok().build()).when(handler).postDeletePurchase(any(ServerRequest.class));

        webClient.post().uri("/purchases/delete-purchase")
                .header(HttpHeaders.AUTHORIZATION, "Basic " + credentials(USER, true))
                .exchange()
                .expectStatus().isOk()
                .expectBody().isEmpty();

        verify(handler, times(1)).postDeletePurchase(any(ServerRequest.class));
    }

    @Test
    void postDeletePurchase_negative_PasswordUserWrongRole() {

        webClient.post().uri("/purchases/delete-purchase")
                .header(HttpHeaders.AUTHORIZATION, "Basic " + credentials(NONE, true))
                .exchange()
                .expectStatus().isForbidden()
                .expectBody().isEmpty();

        verify(handler, times(0)).postDeletePurchase(any(ServerRequest.class));
    }

    @Test
    void postDeletePurchase_positive_Jwt() {

        doReturn(ok().build()).when(handler).postDeletePurchase(any(ServerRequest.class));

        webClient.post().uri("/purchases/delete-purchase")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + testJwt(USER))
                .exchange()
                .expectStatus().isOk()
                .expectBody().isEmpty();

        verify(handler, times(1)).postDeletePurchase(any(ServerRequest.class));
    }

    @Test
    void postDeletePurchase_negative_JwtUserWrongRole() {

        webClient.post().uri("/purchases/delete-purchase")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + testJwt(NONE))
                .exchange()
                .expectStatus().isForbidden()
                .expectBody().isEmpty();

        verify(handler, times(0)).postDeletePurchase(any(ServerRequest.class));
    }

    @Test
    void postCreatePayment_positive_Password() {

        doReturn(ok().build()).when(handler).postCreatePayment(any(ServerRequest.class));

        webClient.post().uri("/payments/create-payment")
                .header(HttpHeaders.AUTHORIZATION, "Basic " + credentials(USER, true))
                .exchange()
                .expectStatus().isOk()
                .expectBody().isEmpty();

        verify(handler, times(1)).postCreatePayment(any(ServerRequest.class));
    }

    @Test
    void postCreatePayment_negative_PasswordUserWrongRole() {

        webClient.post().uri("/payments/create-payment")
                .header(HttpHeaders.AUTHORIZATION, "Basic " + credentials(NONE, true))
                .exchange()
                .expectStatus().isForbidden()
                .expectBody().isEmpty();

        verify(handler, times(0)).postCreatePayment(any(ServerRequest.class));
    }

    @Test
    void postCreatePayment_positive_Jwt() {

        doReturn(ok().build()).when(handler).postCreatePayment(any(ServerRequest.class));

        webClient.post().uri("/payments/create-payment")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + testJwt(USER))
                .exchange()
                .expectStatus().isOk()
                .expectBody().isEmpty();

        verify(handler, times(1)).postCreatePayment(any(ServerRequest.class));
    }

    @Test
    void postCreatePayment_negative_JwtUserWrongRole() {

        webClient.post().uri("/payments/create-payment")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + testJwt(NONE))
                .exchange()
                .expectStatus().isForbidden()
                .expectBody().isEmpty();

        verify(handler, times(0)).postCreatePayment(any(ServerRequest.class));
    }

    @Test
    void getReadPayments_positive_Password() {

        doReturn(ok().build()).when(handler).getReadPayments(any(ServerRequest.class));

        webClient.get().uri("/payments")
                .header(HttpHeaders.AUTHORIZATION, "Basic " + credentials(USER, true))
                .exchange()
                .expectStatus().isOk()
                .expectBody().isEmpty();

        verify(handler, times(1)).getReadPayments(any(ServerRequest.class));
    }

    @Test
    void getReadPayments_negative_PasswordUserWrongRole() {

        webClient.get().uri("/payments")
                .header(HttpHeaders.AUTHORIZATION, "Basic " + credentials(NONE, true))
                .exchange()
                .expectStatus().isForbidden()
                .expectBody().isEmpty();

        verify(handler, times(0)).getReadPayments(any(ServerRequest.class));
    }

    @Test
    void getReadPayments_positive_Jwt() {

        doReturn(ok().build()).when(handler).getReadPayments(any(ServerRequest.class));

        webClient.get().uri("/payments")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + testJwt(USER))
                .exchange()
                .expectStatus().isOk()
                .expectBody().isEmpty();

        verify(handler, times(1)).getReadPayments(any(ServerRequest.class));
    }

    @Test
    void getReadPayments_negative_JwtUserWrongRole() {

        webClient.get().uri("/payments")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + testJwt(NONE))
                .exchange()
                .expectStatus().isForbidden()
                .expectBody().isEmpty();

        verify(handler, times(0)).getReadPayments(any(ServerRequest.class));
    }

    @Test
    void postDeletePayment_positive_Password() {

        doReturn(ok().build()).when(handler).postDeletePayment(any(ServerRequest.class));

        webClient.post().uri("/payments/delete-payment")
                .header(HttpHeaders.AUTHORIZATION, "Basic " + credentials(USER, true))
                .exchange()
                .expectStatus().isOk()
                .expectBody().isEmpty();

        verify(handler, times(1)).postDeletePayment(any(ServerRequest.class));
    }

    @Test
    void postDeletePayment_negative_PasswordUserWrongRole() {

        webClient.post().uri("/payments/delete-payment")
                .header(HttpHeaders.AUTHORIZATION, "Basic " + credentials(NONE, true))
                .exchange()
                .expectStatus().isForbidden()
                .expectBody().isEmpty();

        verify(handler, times(0)).postDeletePayment(any(ServerRequest.class));
    }

    @Test
    void postDeletePayment_positive_Jwt() {

        doReturn(ok().build()).when(handler).postDeletePayment(any(ServerRequest.class));

        webClient.post().uri("/payments/delete-payment")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + testJwt(USER))
                .exchange()
                .expectStatus().isOk()
                .expectBody().isEmpty();

        verify(handler, times(1)).postDeletePayment(any(ServerRequest.class));
    }

    @Test
    void postDeletePayment_negative_JwtUserWrongRole() {

        webClient.post().uri("/payments/delete-payment")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + testJwt(NONE))
                .exchange()
                .expectStatus().isForbidden()
                .expectBody().isEmpty();

        verify(handler, times(0)).postDeletePayment(any(ServerRequest.class));
    }

    @Test
    void getReadAccountBalance_positive_Password() {

        doReturn(ok().build()).when(handler).getReadAccountBalance(any(ServerRequest.class));

        webClient.get().uri("/account-balance")
                .header(HttpHeaders.AUTHORIZATION, "Basic " + credentials(USER, true))
                .exchange()
                .expectStatus().isOk()
                .expectBody().isEmpty();

        verify(handler, times(1)).getReadAccountBalance(any(ServerRequest.class));
    }

    @Test
    void getReadAccountBalance_negative_PasswordUserWrongRole() {

        webClient.get().uri("/account-balance")
                .header(HttpHeaders.AUTHORIZATION, "Basic " + credentials(NONE, true))
                .exchange()
                .expectStatus().isForbidden()
                .expectBody().isEmpty();

        verify(handler, times(0)).getReadAccountBalance(any(ServerRequest.class));
    }

    @Test
    void getReadAccountBalance_positive_Jwt() {

        doReturn(ok().build()).when(handler).getReadAccountBalance(any(ServerRequest.class));

        webClient.get().uri("/account-balance")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + testJwt(USER))
                .exchange()
                .expectStatus().isOk()
                .expectBody().isEmpty();

        verify(handler, times(1)).getReadAccountBalance(any(ServerRequest.class));
    }

    @Test
    void getReadAccountBalance_negative_JwtUserWrongRole() {

        webClient.get().uri("/account-balance")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + testJwt(NONE))
                .exchange()
                .expectStatus().isForbidden()
                .expectBody().isEmpty();

        verify(handler, times(0)).getReadAccountBalance(any(ServerRequest.class));
    }
}

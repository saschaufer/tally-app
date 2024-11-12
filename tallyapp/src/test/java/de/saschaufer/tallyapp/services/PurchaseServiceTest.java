package de.saschaufer.tallyapp.services;

import de.saschaufer.tallyapp.persistence.Persistence;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PurchaseServiceTest {

    private Persistence persistence;
    private PurchaseService purchaseService;

    @BeforeEach
    void beforeEach() {
        persistence = mock(Persistence.class);
        purchaseService = new PurchaseService(persistence);
    }

    @Test
    void createPurchase_positive() {

        doReturn(Mono.empty()).when(persistence).insertPurchase(any(Long.class), any(Long.class));

        purchaseService.createPurchase(1L, 4L)
                .as(StepVerifier::create)
                .verifyComplete();

        verify(persistence, times(1)).insertPurchase(1L, 4L);
    }

    @Test
    void createPurchase_negative() {

        doReturn(Mono.error(new RuntimeException("Error"))).when(persistence).insertPurchase(any(Long.class), any(Long.class));

        purchaseService.createPurchase(1L, 4L)
                .as(StepVerifier::create)
                .verifyErrorSatisfies(error -> {
                    assertThat(error, instanceOf(RuntimeException.class));
                    assertThat(error.getMessage(), containsString("Error"));
                });

        verify(persistence, times(1)).insertPurchase(1L, 4L);
    }

    @Test
    void readPurchases_positive() {

        doReturn(Mono.empty()).when(persistence).selectPurchases(any(Long.class));

        purchaseService.readPurchases(1L)
                .as(StepVerifier::create)
                .verifyComplete();

        verify(persistence, times(1)).selectPurchases(1L);
    }

    @Test
    void readPurchases_negative() {

        doReturn(Mono.error(new RuntimeException("Error"))).when(persistence).selectPurchases(any(Long.class));

        purchaseService.readPurchases(1L)
                .as(StepVerifier::create)
                .verifyErrorSatisfies(error -> {
                    assertThat(error, instanceOf(RuntimeException.class));
                    assertThat(error.getMessage(), containsString("Error"));
                });

        verify(persistence, times(1)).selectPurchases(1L);
    }

    @Test
    void deletePurchase_positive() {

        doReturn(Mono.empty()).when(persistence).deletePurchase(any(Long.class));

        purchaseService.deletePurchase(1L)
                .as(StepVerifier::create)
                .verifyComplete();

        verify(persistence, times(1)).deletePurchase(1L);
    }

    @Test
    void deletePurchase_negative() {

        doReturn(Mono.error(new RuntimeException("Error"))).when(persistence).deletePurchase(any(Long.class));

        purchaseService.deletePurchase(1L)
                .as(StepVerifier::create)
                .verifyErrorSatisfies(error -> {
                    assertThat(error, instanceOf(RuntimeException.class));
                    assertThat(error.getMessage(), containsString("Error"));
                });

        verify(persistence, times(1)).deletePurchase(1L);
    }
}

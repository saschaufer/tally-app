package de.saschaufer.apps.tally.services;

import de.saschaufer.apps.tally.controller.dto.GetPaymentsResponse;
import de.saschaufer.apps.tally.persistence.Persistence;
import de.saschaufer.apps.tally.persistence.dto.Payment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PaymentServiceTest {

    private Persistence persistence;
    private PaymentService paymentService;

    @BeforeEach
    void beforeEach() {
        persistence = mock(Persistence.class);
        paymentService = new PaymentService(persistence);
    }

    @Test
    void createPayment_positive() {

        doReturn(Mono.empty()).when(persistence).insertPayment(any(Payment.class));

        paymentService.createPayment(1L, new BigDecimal("123.45"))
                .as(StepVerifier::create)
                .verifyComplete();

        final ArgumentCaptor<Payment> argumentCaptor = ArgumentCaptor.forClass(Payment.class);

        verify(persistence, times(1)).insertPayment(argumentCaptor.capture());

        final Payment payment = argumentCaptor.getValue();

        assertThat(payment.getId(), nullValue());
        assertThat(payment.getUserId(), is(1L));
        assertThat(payment.getAmount(), is(new BigDecimal("123.45")));
        assertThat(payment.getTimestamp().isAfter(LocalDateTime.now().minusMinutes(1)), is(true));
        assertThat(payment.getTimestamp().isBefore(LocalDateTime.now()), is(true));
    }

    @Test
    void createPayment_negative() {

        doReturn(Mono.error(new RuntimeException("Error"))).when(persistence).insertPayment(any(Payment.class));

        paymentService.createPayment(1L, new BigDecimal("123.45"))
                .as(StepVerifier::create)
                .verifyErrorSatisfies(error -> {
                    assertThat(error, instanceOf(RuntimeException.class));
                    assertThat(error.getMessage(), containsString("Error"));
                });

        verify(persistence, times(1)).insertPayment(any(Payment.class));
    }

    @Test
    void readPayments_positive() {

        doReturn(Mono.just(List.of(
                new GetPaymentsResponse(1L, new BigDecimal("123.45"), LocalDateTime.of(2024, 1, 2, 3, 4, 5)),
                new GetPaymentsResponse(2L, new BigDecimal("678.90"), LocalDateTime.of(2024, 6, 7, 8, 9, 0))
        ))).when(persistence).selectPayments(any(Long.class));


        paymentService.readPayments(1L)
                .as(StepVerifier::create)
                .assertNext(payments -> {

                    assertThat(payments.size(), is(2));

                    assertThat(payments.getFirst().id(), is(1L));
                    assertThat(payments.getFirst().amount(), is(new BigDecimal("123.45")));
                    assertThat(payments.getFirst().timestamp(), is(LocalDateTime.of(2024, 1, 2, 3, 4, 5)));

                    assertThat(payments.getLast().id(), is(2L));
                    assertThat(payments.getLast().amount(), is(new BigDecimal("678.90")));
                    assertThat(payments.getLast().timestamp(), is(LocalDateTime.of(2024, 6, 7, 8, 9, 0)));

                })
                .verifyComplete();

        verify(persistence, times(1)).selectPayments(1L);
    }

    @Test
    void readPayments_negative() {

        doReturn(Mono.error(new RuntimeException("Error"))).when(persistence).selectPayments(any(Long.class));


        paymentService.readPayments(1L)
                .as(StepVerifier::create)
                .verifyErrorSatisfies(error -> {
                    assertThat(error, instanceOf(RuntimeException.class));
                    assertThat(error.getMessage(), containsString("Error"));
                });

        verify(persistence, times(1)).selectPayments(any(Long.class));
    }

    @Test
    void deletePayment_positive() {

        doReturn(Mono.empty()).when(persistence).deletePayment(any(Long.class));

        paymentService.deletePayment(1L)
                .as(StepVerifier::create)
                .verifyComplete();

        verify(persistence, times(1)).deletePayment(1L);
    }

    @Test
    void deletePayment_negative() {

        doReturn(Mono.error(new RuntimeException("Error"))).when(persistence).deletePayment(any(Long.class));

        paymentService.deletePayment(1L)
                .as(StepVerifier::create)
                .verifyErrorSatisfies(error -> {
                    assertThat(error, instanceOf(RuntimeException.class));
                    assertThat(error.getMessage(), containsString("Error"));
                });

        verify(persistence, times(1)).deletePayment(1L);
    }

    @Test
    void readAccountBalance_positive() {

        doReturn(Mono.just(new BigDecimal("123.45"))).when(persistence).selectPaymentsSum(any(Long.class));
        doReturn(Mono.just(new BigDecimal("678.90"))).when(persistence).selectPurchasesSum(any(Long.class));

        paymentService.readAccountBalance(1L)
                .as(StepVerifier::create)
                .assertNext(balance -> {
                    assertThat(balance.amountPayments(), is(new BigDecimal("123.45")));
                    assertThat(balance.amountPurchases(), is(new BigDecimal("678.90")));
                    assertThat(balance.amountTotal(), is(new BigDecimal("-555.45")));
                })
                .verifyComplete();

        verify(persistence, times(1)).selectPaymentsSum(1L);
        verify(persistence, times(1)).selectPurchasesSum(1L);
    }

    @Test
    void readAccountBalance_negative_ErrorOnSelectPaymentsSum() {

        doReturn(Mono.error(new RuntimeException("Error"))).when(persistence).selectPaymentsSum(any(Long.class));
        doReturn(Mono.just(new BigDecimal("678.90"))).when(persistence).selectPurchasesSum(any(Long.class));

        paymentService.readAccountBalance(1L)
                .as(StepVerifier::create)
                .verifyErrorSatisfies(error -> {
                    assertThat(error, instanceOf(RuntimeException.class));
                    assertThat(error.getMessage(), containsString("Error"));
                });

        verify(persistence, times(1)).selectPaymentsSum(1L);
        verify(persistence, times(0)).selectPurchasesSum(any(Long.class));
    }

    @Test
    void readAccountBalance_negative_ErrorOnSelectPurchasesSum() {

        doReturn(Mono.just(new BigDecimal("123.45"))).when(persistence).selectPaymentsSum(any(Long.class));
        doReturn(Mono.error(new RuntimeException("Error"))).when(persistence).selectPurchasesSum(any(Long.class));

        paymentService.readAccountBalance(1L)
                .as(StepVerifier::create)
                .verifyErrorSatisfies(error -> {
                    assertThat(error, instanceOf(RuntimeException.class));
                    assertThat(error.getMessage(), containsString("Error"));
                });

        verify(persistence, times(1)).selectPaymentsSum(1L);
        verify(persistence, times(1)).selectPurchasesSum(1L);
    }
}

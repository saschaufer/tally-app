package de.saschaufer.apps.tally.services;

import de.saschaufer.apps.tally.controller.dto.GetAccountBalanceResponse;
import de.saschaufer.apps.tally.controller.dto.GetPaymentsResponse;
import de.saschaufer.apps.tally.persistence.Persistence;
import de.saschaufer.apps.tally.persistence.dto.Payment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final Persistence persistence;

    public Mono<Void> createPayment(final Long userId, final BigDecimal amount) {
        return persistence.insertPayment(new Payment(null, userId, amount, LocalDateTime.now()));
    }

    public Mono<List<GetPaymentsResponse>> readPayments(final Long userId) {
        return persistence.selectPayments(userId);
    }

    public Mono<Void> deletePayment(final Long paymentId) {
        return persistence.deletePayment(paymentId);
    }

    public Mono<GetAccountBalanceResponse> readAccountBalance(final Long userId) {

        return persistence.selectPaymentsSum(userId)
                .flatMap(paymentsSum -> persistence.selectPurchasesSum(userId)
                        .map(purchasesSum -> Tuples.of(paymentsSum, purchasesSum))
                )
                .map(tuple -> {

                    final BigDecimal payments = tuple.getT1();
                    final BigDecimal purchases = tuple.getT2();
                    final BigDecimal sum = payments.subtract(purchases);

                    return new GetAccountBalanceResponse(payments, purchases, sum);
                });
    }
}

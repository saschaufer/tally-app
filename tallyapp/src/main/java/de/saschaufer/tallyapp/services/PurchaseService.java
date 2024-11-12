package de.saschaufer.tallyapp.services;

import de.saschaufer.tallyapp.controller.dto.GetPurchasesResponse;
import de.saschaufer.tallyapp.persistence.Persistence;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PurchaseService {

    private final Persistence persistence;

    public Mono<Void> createPurchase(final Long userId, final Long productId) {
        return persistence.insertPurchase(userId, productId);
    }

    public Mono<List<GetPurchasesResponse>> readPurchases(final Long userId) {
        return persistence.selectPurchases(userId);
    }

    public Mono<Void> deletePurchase(final Long purchaseId) {
        return persistence.deletePurchase(purchaseId);
    }
}

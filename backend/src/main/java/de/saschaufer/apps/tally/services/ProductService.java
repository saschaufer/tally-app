package de.saschaufer.apps.tally.services;

import de.saschaufer.apps.tally.controller.dto.GetProductsResponse;
import de.saschaufer.apps.tally.persistence.Persistence;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final Persistence persistence;

    public Mono<Void> createProduct(final String name, final BigDecimal price) {
        return persistence.insertProductAndPrice(name, price);
    }

    public Mono<List<GetProductsResponse>> readProducts() {
        return persistence.selectProducts()
                .map(products -> products.stream()
                        .map(tuple -> {

                            final Long id = tuple.getT1().getId();
                            final String name = tuple.getT1().getName();
                            final BigDecimal price = tuple.getT2().getPrice();

                            return new GetProductsResponse(id, name, price);
                        })
                        .toList()
                )
                .switchIfEmpty(Mono.just(List.of()));
    }

    public Mono<Void> updateProduct(final Long id, final String newName) {
        return persistence.updateProduct(id, newName);
    }

    public Mono<Void> updateProductPrice(final Long productId, final BigDecimal price) {
        return persistence.updateProductPrice(productId, price);
    }
}
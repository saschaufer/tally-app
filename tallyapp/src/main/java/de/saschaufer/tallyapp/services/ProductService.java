package de.saschaufer.tallyapp.services;

import de.saschaufer.tallyapp.controller.dto.GetProductsResponse;
import de.saschaufer.tallyapp.persistence.Persistence;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final Persistence persistence;

    public Mono<Void> createProduct(final String name, final BigDecimal price) {

        return persistence.existsProduct(name)
                .flatMap(found -> {

                    if (found.equals(Boolean.FALSE)) {
                        return persistence.insertProductAndPrice(name, price);
                    }

                    return persistence.selectProduct(name)
                            .flatMap(p -> persistence.existsProductPrice(p.getId())
                                    .flatMap(foundPrice -> {

                                        if (foundPrice.equals(Boolean.FALSE)) {
                                            return persistence.insertProductPrice(p.getId(), price)
                                                    .then(Mono.empty());
                                        }

                                        return Mono.error(new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Product already exists"));
                                    }));
                });
    }

    public Mono<GetProductsResponse> readProduct(final Long productId) {
        return persistence.selectProduct(productId)
                .map(product -> {

                    final Long id = product.getT1().getId();
                    final String name = product.getT1().getName();
                    final BigDecimal price = product.getT2().getPrice();

                    return new GetProductsResponse(id, name, price);
                });
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

    public Mono<Void> deleteProduct(final Long id) {
        return persistence.deleteProduct(id);
    }

    public Mono<Void> updateProductPrice(final Long productId, final BigDecimal price) {
        return persistence.updateProductPrice(productId, price);
    }
}

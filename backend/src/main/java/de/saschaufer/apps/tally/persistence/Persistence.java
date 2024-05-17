package de.saschaufer.apps.tally.persistence;

import de.saschaufer.apps.tally.persistence.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

import static org.springframework.data.relational.core.query.Criteria.where;
import static org.springframework.data.relational.core.query.Query.query;
import static org.springframework.data.relational.core.query.Update.update;

@Component
@RequiredArgsConstructor
public class Persistence {

    private final R2dbcEntityTemplate template;
    private final ReactiveTransactionManager transactionManager;

    public Mono<User> insertUser(final User user) {
        return Mono.just(user).flatMap(template::insert);
    }

    public Mono<User> selectUser(final String username) {
        return template.selectOne(query(where("username").is(username)), User.class)
                .switchIfEmpty(Mono.error(new RuntimeException("User not found")));
    }

    public Mono<Boolean> existsUser(final String username) {
        return template.exists(query(where("username").is(username)), User.class);
    }

    public Mono<Void> updateUserPassword(final Long id, final String newPassword) {

        return template

                // Update user
                .update(User.class)
                .matching(query(where("id").is(id)))
                .apply(update("password", newPassword))

                // If no user was updated, return an error
                .flatMap(updateCount -> switch (updateCount.intValue()) {
                    case 0 -> Mono.error(new RuntimeException("User not updated"));
                    case 1 -> Mono.empty();
                    default -> Mono.error(new RuntimeException("Too many users updated"));
                });
    }

    public Mono<Product> insertProduct(final Product product) {
        return Mono.just(product).flatMap(template::insert);
    }

    public Mono<ProductPrice> insertProductPrice(final ProductPrice productPrice) {
        return Mono.just(productPrice).flatMap(template::insert);
    }

    public Mono<ProductPrice> insertProductAndPrice(final Product product, final ProductPrice productPrice) {

        final TransactionalOperator trans = TransactionalOperator.create(transactionManager);

        return trans.transactional(Mono.just(product)
                .flatMap(this::insertProduct)
                .map(p -> {
                    productPrice.setProductId(p.getId());
                    return productPrice;
                })
                .flatMap(this::insertProductPrice)
        );
    }

    public Mono<Purchase> insertPurchase(final Purchase purchase) {
        return Mono.just(purchase).flatMap(template::insert);
    }

    public Mono<Payment> insertPayment(final Payment payment) {
        return Mono.just(payment).flatMap(template::insert);
    }
}

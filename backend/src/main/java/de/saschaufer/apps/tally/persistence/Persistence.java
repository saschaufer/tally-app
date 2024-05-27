package de.saschaufer.apps.tally.persistence;

import de.saschaufer.apps.tally.controller.dto.GetPurchasesResponse;
import de.saschaufer.apps.tally.persistence.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

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

    public Mono<Void> insertProductAndPrice(final String name, final BigDecimal price) {

        final Product product = new Product(null, name);
        final ProductPrice productPrice = new ProductPrice(null, null, price, null);

        final TransactionalOperator trans = TransactionalOperator.create(transactionManager);

        return trans.transactional(Mono.just(product)
                        .flatMap(template::insert)
                        .map(p -> {
                            productPrice.setProductId(p.getId());
                            return productPrice;
                        })
                        .flatMap(template::insert)
                )
                .flatMap(p -> Mono.empty());
    }

    public Mono<Tuple2<Product, ProductPrice>> selectProduct(final Long productId) {

        final String query = """
                select products.id, products.name, product_prices.price from products
                right join product_prices on products.id = product_prices.product_id
                where products.id = :product_id
                and product_prices.valid_until is null
                order by products.name
                """.toLowerCase();

        return template.getDatabaseClient().sql(query)
                .bind("product_id", productId)
                .map((row, rowMetadata) -> {
                    final Long id = Objects.requireNonNull(row.get("id", Integer.class)).longValue();
                    final String name = row.get("name", String.class);
                    final BigDecimal price = row.get("price", BigDecimal.class);
                    return Tuples.of(new Product(id, name), new ProductPrice(null, id, price, null));
                }).one()
                .switchIfEmpty(Mono.error(new RuntimeException("Product not found")));
    }

    public Mono<List<Tuple2<Product, ProductPrice>>> selectProducts() {

        final String query = """
                select products.id, products.name, product_prices.price from products
                right join product_prices on products.id = product_prices.product_id
                where product_prices.valid_until is null
                order by products.name
                """.toLowerCase();

        return template.getDatabaseClient().sql(query).map((row, rowMetadata) -> {
            final Long id = Objects.requireNonNull(row.get("id", Integer.class)).longValue();
            final String name = row.get("name", String.class);
            final BigDecimal price = row.get("price", BigDecimal.class);
            return Tuples.of(new Product(id, name), new ProductPrice(null, id, price, null));
        }).all().collectList();
    }

    public Mono<Void> updateProduct(final Long id, final String newName) {

        return template
                .update(Product.class)
                .matching(query(where("id").is(id)))
                .apply(update("name", newName))

                .flatMap(updateCount -> switch (updateCount.intValue()) {
                    case 0 -> Mono.error(new RuntimeException("Product not updated"));
                    case 1 -> Mono.empty();
                    default -> Mono.error(new RuntimeException("Too many products updated"));
                });
    }

    public Mono<Void> updateProductPrice(final Long productId, final BigDecimal productPrice) {

        final TransactionalOperator trans = TransactionalOperator.create(transactionManager);

        return trans.transactional(template
                        .update(ProductPrice.class)
                        .matching(query(where("product_id").is(productId).and(where("valid_until").isNull())))
                        .apply(update("valid_until", LocalDateTime.now()))
                        .flatMap(count -> Mono.just(new ProductPrice(null, productId, productPrice, null)))
                        .flatMap(template::insert)
                )
                .flatMap(p -> Mono.empty());
    }

    public Mono<Void> insertPurchase(final Long userId, final Long productId) {

        return template.selectOne(query(where("product_id").is(productId)
                        .and(where("valid_until").isNull())), ProductPrice.class)
                .switchIfEmpty(Mono.error(new Exception("Product price not found")))
                .map(price -> new Purchase(null, userId, price.getId(), LocalDateTime.now()))
                .flatMap(template::insert)
                .flatMap(p -> Mono.empty());
    }

    public Mono<List<GetPurchasesResponse>> selectPurchases(final Long userId) {

        final String query = """
                select purchases.id, purchases.timestamp, product_prices.price, products.name
                from purchases
                    left join product_prices on product_prices.id = purchases.product_price_id
                    left join products on products.id = product_prices.product_id
                where purchases.user_id = :user_id
                order by products.name
                """.toLowerCase();

        return template.getDatabaseClient().sql(query)
                .bind("user_id", userId)
                .map((row, rowMetadata) -> {
                    final Long id = Objects.requireNonNull(row.get("id", Integer.class)).longValue();
                    final LocalDateTime timestamp = row.get("timestamp", LocalDateTime.class);
                    final String name = row.get("name", String.class);
                    final BigDecimal price = row.get("price", BigDecimal.class);

                    return new GetPurchasesResponse(id, timestamp, name, price);
                }).all().collectList();
    }

    public Mono<Void> deletePurchase(final Long purchaseId) {

        return template
                .delete(Purchase.class)
                .matching(query(where("id").is(purchaseId)))
                .all()

                .flatMap(deleteCount -> switch (deleteCount.intValue()) {
                    case 0 -> Mono.error(new RuntimeException("Purchase not deleted"));
                    case 1 -> Mono.empty();
                    default -> Mono.error(new RuntimeException("Too many purchases deleted"));
                });
    }

    public Mono<Payment> insertPayment(final Payment payment) {
        return Mono.just(payment).flatMap(template::insert);
    }
}

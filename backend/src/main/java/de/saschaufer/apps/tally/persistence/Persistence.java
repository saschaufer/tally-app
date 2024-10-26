package de.saschaufer.apps.tally.persistence;

import de.saschaufer.apps.tally.controller.dto.GetPaymentsResponse;
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
import java.util.Map;
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

    public Mono<User> selectUser(final String email) {
        return template.selectOne(query(where("email").is(email)), User.class)
                .switchIfEmpty(Mono.error(new RuntimeException("User not found")));
    }

    public Mono<List<User>> selectUsers() {
        return template.select(User.class)
                .all()
                .map(user -> new User(
                                user.getId(),
                                user.getEmail(),
                                null,
                                user.getRoles(),
                                null,
                                user.getRegistrationOn(), user.getRegistrationComplete()
                        )
                )
                .collectList();
    }

    public Mono<Boolean> existsUser(final String email) {
        return template.exists(query(where("email").is(email)), User.class);
    }

    public Mono<Void> deleteUser(final Long userId) {

        final TransactionalOperator trans = TransactionalOperator.create(transactionManager);

        return trans.transactional(Mono.just(userId)
                        .flatMap(i -> template.delete(query(where("user_id").is(userId)), Purchase.class))
                        .flatMap(i -> template.delete(query(where("user_id").is(userId)), Payment.class))
                        .flatMap(i -> template.delete(query(where("id").is(userId)), User.class))
                        .flatMap(deleteCount -> switch (deleteCount.intValue()) {
                            case 0 -> Mono.error(new RuntimeException("User not deleted"));
                            case 1 -> Mono.empty();
                            default -> Mono.error(new RuntimeException("Too many users deleted"));
                        })
                )
                .flatMap(p -> Mono.empty());
    }

    public Mono<Void> updateUserRegistrationComplete(final String email) {

        return template

                // Update user
                .update(User.class)
                .matching(query(where("email").is(email)))
                .apply(update("registration_complete", true))

                // If no user was updated, return an error
                .flatMap(updateCount -> switch (updateCount.intValue()) {
                    case 0 -> Mono.error(new RuntimeException("User not updated"));
                    case 1 -> Mono.empty();
                    default -> Mono.error(new RuntimeException("Too many users updated"));
                });
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

    public Mono<Long> deleteUnregisteredUsers(final LocalDateTime registrationBefore) {

        return template
                .delete(User.class)
                .matching(query(where("registration_complete").isFalse()
                        .and("registration_on").lessThan(registrationBefore)))
                .all();
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

    public Mono<Product> selectProduct(final String name) {
        return template.selectOne(query(where("name").is(name)), Product.class)
                .switchIfEmpty(Mono.error(new RuntimeException("Product not found")));
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

    public Mono<Boolean> existsProduct(final String name) {
        return template.exists(query(where("name").is(name)), Product.class);
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

    public Mono<Void> deleteProduct(final Long id) {

        return template
                .update(ProductPrice.class)
                .matching(query(where("product_id").is(id).and(where("valid_until").isNull())))
                .apply(update("valid_until", LocalDateTime.now()))
                .flatMap(updateCount -> switch (updateCount.intValue()) {
                    case 0 -> Mono.error(new RuntimeException("Product not deleted"));
                    case 1 -> Mono.empty();
                    default -> Mono.error(new RuntimeException("Too many products deleted"));
                });
    }

    public Mono<ProductPrice> insertProductPrice(final Long productId, final BigDecimal productPrice) {
        return Mono.just(new ProductPrice(null, productId, productPrice, null)).flatMap(template::insert);
    }

    public Mono<Boolean> existsProductPrice(final Long productId) {
        return template.exists(query(where("product_id").is(productId).and(where("valid_until").isNull())), ProductPrice.class);
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

    public Mono<BigDecimal> selectPurchasesSum(final Long userId) {

        final String query = """
                select sum(product_prices.price) as sum
                from purchases
                    left join product_prices on product_prices.id = purchases.product_price_id
                    left join products on products.id = product_prices.product_id
                where purchases.user_id = :user_id
                """.toLowerCase();

        return template.getDatabaseClient().sql(query)
                .bind("user_id", userId)
                .map((row, rowMetadata) -> {
                            final BigDecimal sum = row.get("sum", BigDecimal.class);
                            return sum == null ? BigDecimal.ZERO : sum;
                        }
                )
                .one();
    }

    public Mono<Map<Long, BigDecimal>> selectPurchasesSumAllUsers() {

        final String query = """
                select user_id, sum(product_prices.price) as sum
                from purchases
                    left join product_prices on product_prices.id = purchases.product_price_id
                    left join products on products.id = product_prices.product_id
                group by user_id;
                """.toLowerCase();

        return template.getDatabaseClient().sql(query)
                .map((row, rowMetadata) -> {
                            final Long userId = Objects.requireNonNull(row.get("user_id", Integer.class)).longValue();
                            final BigDecimal sum = row.get("sum", BigDecimal.class);
                            return Tuples.of(userId, sum == null ? BigDecimal.ZERO : sum);
                        }
                )
                .all().collectMap(Tuple2::getT1, Tuple2::getT2);
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

    public Mono<Void> insertPayment(final Payment payment) {
        return Mono.just(payment).flatMap(template::insert).flatMap(p -> Mono.empty());
    }

    public Mono<List<GetPaymentsResponse>> selectPayments(final Long userId) {
        return template.select(query(where("user_id").is(userId)), Payment.class)
                .map(payment -> new GetPaymentsResponse(payment.getId(), payment.getAmount(), payment.getTimestamp()))
                .collectList();
    }

    public Mono<BigDecimal> selectPaymentsSum(final Long userId) {

        final String query = """
                select sum(amount) as sum
                from payments
                where user_id = :user_id
                """.toLowerCase();

        return template.getDatabaseClient().sql(query)
                .bind("user_id", userId)
                .map((row, rowMetadata) -> {
                            final BigDecimal sum = row.get("sum", BigDecimal.class);
                            return sum == null ? BigDecimal.ZERO : sum;
                        }
                )
                .one();
    }

    public Mono<Map<Long, BigDecimal>> selectPaymentsSumAllUsers() {

        final String query = """
                select user_id, sum(amount) as sum
                from payments
                group by user_id;
                """.toLowerCase();

        return template.getDatabaseClient().sql(query)
                .map((row, rowMetadata) -> {
                            final Long userId = Objects.requireNonNull(row.get("user_id", Integer.class)).longValue();
                            final BigDecimal sum = row.get("sum", BigDecimal.class);
                            return Tuples.of(userId, sum == null ? BigDecimal.ZERO : sum);
                        }
                )
                .all().collectMap(Tuple2::getT1, Tuple2::getT2);
    }

    public Mono<Void> deletePayment(final Long paymentId) {
        return template
                .delete(Payment.class)
                .matching(query(where("id").is(paymentId)))
                .all()

                .flatMap(deleteCount -> switch (deleteCount.intValue()) {
                    case 0 -> Mono.error(new RuntimeException("Payment not deleted"));
                    case 1 -> Mono.empty();
                    default -> Mono.error(new RuntimeException("Too many payments deleted"));
                });
    }
}

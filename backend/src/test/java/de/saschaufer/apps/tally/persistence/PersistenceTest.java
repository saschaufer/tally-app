package de.saschaufer.apps.tally.persistence;

import de.saschaufer.apps.tally.config.db.DbConfig;
import de.saschaufer.apps.tally.config.db.DbProperties;
import de.saschaufer.apps.tally.persistence.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.boot.web.reactive.error.DefaultErrorAttributes;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.data.relational.core.query.Criteria.where;
import static org.springframework.data.relational.core.query.Query.empty;
import static org.springframework.data.relational.core.query.Query.query;

@Slf4j
@DataR2dbcTest
@Import({PersistenceTest.TestDbProperties.class, DbConfig.class, Persistence.class, DefaultErrorAttributes.class})
class PersistenceTest {

    public static class TestDbProperties {

        @Bean
        public DbProperties dbProperties() {
            return new DbProperties("r2dbc:h2:mem:///test?options=DB_CLOSE_DELAY=-1");
        }
    }

    @Autowired
    private Persistence persistence;

    @Autowired
    private R2dbcEntityTemplate template;

    @BeforeEach
    void beforeEach() {
        Mono.just(1)
                .flatMap(m -> template.delete(empty(), Payment.class))
                .flatMap(m -> template.delete(empty(), Purchase.class))
                .flatMap(m -> template.delete(empty(), ProductPrice.class))
                .flatMap(m -> template.delete(empty(), Product.class))
                .flatMap(m -> template.delete(empty(), User.class))
                .subscribe(
                        ok -> log.atInfo().setMessage("Deleted all entities.").log(),
                        err -> log.atInfo().setMessage("Error deleting all entities.").setCause(err).log()
                )
        ;
    }

    @Test
    void insertUser_positive() {

        assertCount(User.class, 0);

        Mono.just(new User(null, "test-username", "test-password", "test-role"))
                .flatMap(persistence::insertUser)
                .as(StepVerifier::create)
                .assertNext(user -> {
                    assertThat(user.getId(), notNullValue());
                    assertThat(user.getUsername(), is("test-username"));
                    assertThat(user.getPassword(), is("test-password"));
                    assertThat(user.getRoles(), is("test-role"));
                })
                .verifyComplete()
        ;

        assertCount(User.class, 1);
    }

    @Test
    void insertUser_negative_DuplicatePrimaryKey() {

        assertCount(User.class, 0);

        Mono.just(new User(1L, "", "", ""))
                .flatMap(persistence::insertUser)
                .flatMap(persistence::insertUser)
                .as(StepVerifier::create)
                .verifyErrorSatisfies(error ->
                        assertThat(error.getMessage(), containsString("Unique index or primary key violation: \"PRIMARY KEY ON PUBLIC.USERS(ID)"))
                )
        ;

        assertCount(User.class, 1);
    }

    @ParameterizedTest
    @MethodSource
    void insertUser_negative_NullNotAllowed(final User user, final String errorMessage) {

        assertCount(User.class, 0);

        Mono.just(user)
                .flatMap(persistence::insertUser)
                .as(StepVerifier::create)
                .verifyErrorSatisfies(error ->
                        assertThat(error.getMessage(), containsString(errorMessage))
                )
        ;

        assertCount(User.class, 0);
    }

    static Stream<Arguments> insertUser_negative_NullNotAllowed() {
        return Stream.of(
                Arguments.of(
                        new User(null, null, "", ""),
                        "NULL not allowed for column \"USERNAME\""
                ),
                Arguments.of(
                        new User(null, "", null, ""),
                        "NULL not allowed for column \"PASSWORD\""
                ),
                Arguments.of(
                        new User(null, "", "", null),
                        "NULL not allowed for column \"ROLES\""
                )
        );
    }

    @Test
    void selectUser_positive_UserExists() {

        final String username = Objects.requireNonNull(persistence.insertUser(
                new User(null, "test-username", "test-password", "test-role")
        ).block()).getUsername();

        Mono.just(username)
                .flatMap(persistence::selectUser)
                .as(StepVerifier::create)
                .assertNext(user -> {
                    assertThat(user.getId(), notNullValue());
                    assertThat(user.getUsername(), is("test-username"));
                    assertThat(user.getPassword(), is("test-password"));
                    assertThat(user.getRoles(), is("test-role"));
                })
                .verifyComplete();
    }

    @Test
    void selectUser_negative_UserNotExists() {

        Mono.just("username")
                .flatMap(persistence::selectUser)
                .as(StepVerifier::create)
                .verifyErrorSatisfies(error ->
                        assertThat(error.getMessage(), containsString("User not found"))
                );
    }

    @Test
    void existsUser_positive_UserExists() {

        final String username = Objects.requireNonNull(persistence.insertUser(
                new User(null, "test-username", "test-password", "test-role")
        ).block()).getUsername();

        Mono.just(username)
                .flatMap(persistence::existsUser)
                .as(StepVerifier::create)
                .assertNext(exists -> assertThat(exists, is(true)))
                .verifyComplete();
    }

    @Test
    void existsUser_positive_UserNotExists() {

        Mono.just("username")
                .flatMap(persistence::existsUser)
                .as(StepVerifier::create)
                .assertNext(exists -> assertThat(exists, is(false)))
                .verifyComplete();
    }

    @Test
    void updateUserPassword_positive_UserExists() {

        final Long userId = Objects.requireNonNull(persistence.insertUser(
                new User(null, "test-username", "test-password", "test-role")
        ).block()).getId();

        Mono.just(userId)
                .flatMap(id -> persistence.updateUserPassword(id, "test-password-changed"))
                .as(StepVerifier::create)
                .expectNext()
                .verifyComplete();

        Mono.just("test-username")
                .flatMap(persistence::selectUser)
                .as(StepVerifier::create)
                .assertNext(user -> {
                    assertThat(user.getId(), notNullValue());
                    assertThat(user.getUsername(), is("test-username"));
                    assertThat(user.getPassword(), is("test-password-changed"));
                    assertThat(user.getRoles(), is("test-role"));
                })
                .verifyComplete();
    }

    @Test
    void updateUserPassword_negative_UserNotExists() {

        Mono.just(1L)
                .flatMap(id -> persistence.updateUserPassword(id, ""))
                .as(StepVerifier::create)
                .expectNext()
                .verifyErrorSatisfies(error ->
                        assertThat(error.getMessage(), containsString("User not updated"))
                );
    }

    @Test
    void insertProductAndPrice_positive() {

        assertCount(Product.class, 0);
        assertCount(ProductPrice.class, 0);

        persistence.insertProductAndPrice("test-name", new BigDecimal("123.45"))
                .as(StepVerifier::create)
                .verifyComplete();

        assertCount(Product.class, 1);
        assertCount(ProductPrice.class, 1);
    }

    @ParameterizedTest
    @MethodSource
    void insertProductAndPrice_negative_rollback(final String name, final BigDecimal price, final String expectedErrorMessage) {

        assertCount(Product.class, 0);
        assertCount(ProductPrice.class, 0);

        persistence.insertProductAndPrice(name, price)
                .as(StepVerifier::create)
                .verifyErrorSatisfies(error ->
                        assertThat(error.getMessage(), containsString(expectedErrorMessage))
                );

        assertCount(Product.class, 0);
        assertCount(ProductPrice.class, 0);
    }

    static Stream<Arguments> insertProductAndPrice_negative_rollback() {
        return Stream.of(
                Arguments.of(null, new BigDecimal("123.45"), "executeMany; bad SQL grammar [INSERT INTO products VALUES (DEFAULT)]"),
                Arguments.of("test-name", null, "NULL not allowed for column \"PRICE\"")
        );
    }

    @Test
    void insertProductAndPrice_negative_rollback_ProductNameExists() {

        assertCount(Product.class, 0);
        assertCount(ProductPrice.class, 0);

        persistence.insertProductAndPrice("test-name", new BigDecimal("123.45"))
                .then(persistence.insertProductAndPrice("test-name", new BigDecimal("123.45")))
                .as(StepVerifier::create)
                .verifyErrorSatisfies(error ->
                        assertThat(error.getMessage(), containsString("Unique index or primary key violation"))
                );

        assertCount(Product.class, 1);
        assertCount(ProductPrice.class, 1);
    }

    @Test
    void selectProducts_positive_NoProductsExist() {

        persistence.selectProducts()
                .as(StepVerifier::create)
                .assertNext(products ->
                        assertThat(products.isEmpty(), is(true))
                )
                .verifyComplete();
    }

    @Test
    void selectProducts_positive_ProductsExist() {

        final Mono<Void> coffee = template.insert(new Product(null, "coffee"))
                .flatMap(product ->
                        template.insert(new ProductPrice(null, product.getId(), new BigDecimal("0.1"), null))
                                .then(template.insert(new ProductPrice(null, product.getId(), new BigDecimal("0.2"), LocalDateTime.now())))
                                .then(template.insert(new ProductPrice(null, product.getId(), new BigDecimal("0.3"), LocalDateTime.now())))
                )
                .flatMap(p -> Mono.empty());

        final Mono<Void> tea = template.insert(new Product(null, "tea"))
                .flatMap(product ->
                        template.insert(new ProductPrice(null, product.getId(), new BigDecimal("0.4"), null))
                                .then(template.insert(new ProductPrice(null, product.getId(), new BigDecimal("0.5"), LocalDateTime.now())))
                                .then(template.insert(new ProductPrice(null, product.getId(), new BigDecimal("0.6"), LocalDateTime.now())))
                )
                .flatMap(p -> Mono.empty());

        final Mono<Void> hotChocolate = template.insert(new Product(null, "hot chocolate"))
                .flatMap(product ->
                        template.insert(new ProductPrice(null, product.getId(), new BigDecimal("0.7"), LocalDateTime.now()))
                                .then(template.insert(new ProductPrice(null, product.getId(), new BigDecimal("0.8"), LocalDateTime.now())))
                                .then(template.insert(new ProductPrice(null, product.getId(), new BigDecimal("0.9"), LocalDateTime.now())))
                )
                .flatMap(p -> Mono.empty());

        coffee.then(tea).then(hotChocolate).then(persistence.selectProducts())
                .as(StepVerifier::create)
                .assertNext(products -> {
                    assertThat(products.size(), is(2));

                    assertThat(products.getFirst().getT1().getId(), notNullValue());
                    assertThat(products.getFirst().getT1().getName(), is("coffee"));
                    assertThat(products.getFirst().getT2().getPrice(), is(new BigDecimal("0.10")));
                    assertThat(products.getFirst().getT2().getValidUntil(), nullValue());

                    assertThat(products.get(1).getT1().getId(), notNullValue());
                    assertThat(products.get(1).getT1().getName(), is("tea"));
                    assertThat(products.get(1).getT2().getPrice(), is(new BigDecimal("0.40")));
                    assertThat(products.get(1).getT2().getValidUntil(), nullValue());
                })
                .verifyComplete();
    }

    @Test
    void updateProduct_positive() {

        final Long p1Id = Objects.requireNonNull(template.insert(new Product(null, "coffee")).block()).getId();
        final Long p2Id = Objects.requireNonNull(template.insert(new Product(null, "tea")).block()).getId();

        persistence.updateProduct(p1Id, "coffee-new")
                .then(template.selectOne(query(where("id").is(p1Id)), Product.class))
                .as(StepVerifier::create)
                .assertNext(product -> {
                    assertThat(product.getId(), notNullValue());
                    assertThat(product.getName(), is("coffee-new"));
                })
                .verifyComplete();

        template.selectOne(query(where("id").is(p2Id)), Product.class)
                .as(StepVerifier::create)
                .assertNext(product -> {
                    assertThat(product.getId(), notNullValue());
                    assertThat(product.getName(), is("tea"));
                })
                .verifyComplete();
    }

    @Test
    void updateProduct_negative_ProductNotExist() {

        persistence.updateProduct(1L, "new")
                .as(StepVerifier::create)
                .verifyErrorSatisfies(error ->
                        assertThat(error.getMessage(), containsString("Product not updated"))
                );
    }

    @Test
    void updateProductPrice_positive_PriceExists() {

        final Long p1Id = Objects.requireNonNull(template.insert(new Product(null, "coffee"))
                .flatMap(product ->
                        template.insert(new ProductPrice(null, product.getId(), new BigDecimal("0.1"), null))
                                .then(template.insert(new ProductPrice(null, product.getId(), new BigDecimal("0.2"), LocalDateTime.now())))
                                .then(template.insert(new ProductPrice(null, product.getId(), new BigDecimal("0.3"), LocalDateTime.now())))
                                .then(Mono.just(product))
                ).block()).getId();

        template.insert(new Product(null, "tea"))
                .flatMap(product ->
                        template.insert(new ProductPrice(null, product.getId(), new BigDecimal("0.4"), null))
                                .then(template.insert(new ProductPrice(null, product.getId(), new BigDecimal("0.5"), LocalDateTime.now())))
                                .then(template.insert(new ProductPrice(null, product.getId(), new BigDecimal("0.6"), LocalDateTime.now())))
                                .then(Mono.just(product))
                ).block();

        persistence.updateProductPrice(p1Id, BigDecimal.TEN)
                .as(StepVerifier::create)
                .verifyComplete();

        persistence.selectProducts()
                .as(StepVerifier::create)
                .assertNext(products -> {
                    assertThat(products.size(), is(2));

                    assertThat(products.getFirst().getT1().getId(), notNullValue());
                    assertThat(products.getFirst().getT1().getName(), is("coffee"));
                    assertThat(products.getFirst().getT2().getPrice(), is(new BigDecimal("10.00")));
                    assertThat(products.getFirst().getT2().getValidUntil(), nullValue());

                    assertThat(products.get(1).getT1().getId(), notNullValue());
                    assertThat(products.get(1).getT1().getName(), is("tea"));
                    assertThat(products.get(1).getT2().getPrice(), is(new BigDecimal("0.40")));
                    assertThat(products.get(1).getT2().getValidUntil(), nullValue());
                })
                .verifyComplete();
    }

    @Test
    void updateProductPrice_positive_PriceNotExists() {

        final Long p1Id = Objects.requireNonNull(template.insert(new Product(null, "coffee"))
                .flatMap(product ->
                        template.insert(new ProductPrice(null, product.getId(), new BigDecimal("0.1"), LocalDateTime.now()))
                                .then(template.insert(new ProductPrice(null, product.getId(), new BigDecimal("0.2"), LocalDateTime.now())))
                                .then(template.insert(new ProductPrice(null, product.getId(), new BigDecimal("0.3"), LocalDateTime.now())))
                                .then(Mono.just(product))
                ).block()).getId();

        template.insert(new Product(null, "tea"))
                .flatMap(product ->
                        template.insert(new ProductPrice(null, product.getId(), new BigDecimal("0.4"), null))
                                .then(template.insert(new ProductPrice(null, product.getId(), new BigDecimal("0.5"), LocalDateTime.now())))
                                .then(template.insert(new ProductPrice(null, product.getId(), new BigDecimal("0.6"), LocalDateTime.now())))
                                .then(Mono.just(product))
                ).block();

        persistence.updateProductPrice(p1Id, BigDecimal.TEN)
                .as(StepVerifier::create)
                .verifyComplete();

        persistence.selectProducts()
                .as(StepVerifier::create)
                .assertNext(products -> {
                    assertThat(products.size(), is(2));

                    assertThat(products.getFirst().getT1().getId(), notNullValue());
                    assertThat(products.getFirst().getT1().getName(), is("coffee"));
                    assertThat(products.getFirst().getT2().getPrice(), is(new BigDecimal("10.00")));
                    assertThat(products.getFirst().getT2().getValidUntil(), nullValue());

                    assertThat(products.get(1).getT1().getId(), notNullValue());
                    assertThat(products.get(1).getT1().getName(), is("tea"));
                    assertThat(products.get(1).getT2().getPrice(), is(new BigDecimal("0.40")));
                    assertThat(products.get(1).getT2().getValidUntil(), nullValue());
                })
                .verifyComplete();
    }

    @Test
    void updateProductPrice_negative_ProductNotExist() {

        final Long p1Id = Objects.requireNonNull(template.insert(new Product(null, "coffee"))
                .flatMap(product ->
                        template.insert(new ProductPrice(null, product.getId(), new BigDecimal("0.1"), null))
                                .then(template.insert(new ProductPrice(null, product.getId(), new BigDecimal("0.2"), LocalDateTime.now())))
                                .then(template.insert(new ProductPrice(null, product.getId(), new BigDecimal("0.3"), LocalDateTime.now())))
                                .then(Mono.just(product))
                ).block()).getId();

        persistence.updateProductPrice(p1Id + 1, BigDecimal.TEN)
                .as(StepVerifier::create)
                .verifyErrorSatisfies(error ->
                        assertThat(error.getMessage(), containsString("Referential integrity constraint violation"))
                );

        persistence.selectProducts()
                .as(StepVerifier::create)
                .assertNext(products -> {
                    assertThat(products.size(), is(1));

                    assertThat(products.getFirst().getT1().getId(), notNullValue());
                    assertThat(products.getFirst().getT1().getName(), is("coffee"));
                    assertThat(products.getFirst().getT2().getPrice(), is(new BigDecimal("0.10")));
                    assertThat(products.getFirst().getT2().getValidUntil(), nullValue());
                })
                .verifyComplete();
    }

    //////

    @Test
    void insertPurchase_positive() {

        final Long userId = Objects.requireNonNull(persistence.insertUser(
                new User(null, "", "", "")
        ).block()).getId();

        final Long productPriceId = Objects.requireNonNull(template.insert(new Product(null, "coffee"))
                .flatMap(product -> template
                        .insert(new ProductPrice(null, product.getId(), BigDecimal.ONE, null))
                ).block()).getId();

        assertCount(Purchase.class, 0);

        Mono.just(new Purchase(null, userId, productPriceId, LocalDateTime.of(2024, 2, 20, 14, 59, 2)))
                .flatMap(persistence::insertPurchase)
                .as(StepVerifier::create)
                .assertNext(purchase -> {
                    assertThat(purchase.getId(), notNullValue());
                    assertThat(purchase.getUserId(), is(userId));
                    assertThat(purchase.getProductPriceId(), is(productPriceId));
                    assertThat(purchase.getTimestamp(), is(LocalDateTime.of(2024, 2, 20, 14, 59, 2)));
                })
                .verifyComplete()
        ;

        assertCount(Purchase.class, 1);
    }

    @Test
    void insertPurchase_negative_DuplicatePrimaryKey() {

        final Long userId = Objects.requireNonNull(persistence.insertUser(
                new User(null, "", "", "")
        ).block()).getId();

        final Long productPriceId = Objects.requireNonNull(template.insert(new Product(null, "coffee"))
                .flatMap(product -> template
                        .insert(new ProductPrice(null, product.getId(), BigDecimal.ONE, null))
                ).block()).getId();

        assertCount(Purchase.class, 0);

        Mono.just(new Purchase(1L, userId, productPriceId, LocalDateTime.of(2024, 2, 20, 14, 59, 2)))
                .flatMap(persistence::insertPurchase)
                .flatMap(persistence::insertPurchase)
                .as(StepVerifier::create)
                .verifyErrorSatisfies(error ->
                        assertThat(error.getMessage(), containsString("Unique index or primary key violation: \"PRIMARY KEY ON PUBLIC.PURCHASES(ID)"))
                )
        ;

        assertCount(Purchase.class, 1);
    }

    @Test
    void insertPurchase_negative_UserIdNull() {

        final Long productPriceId = Objects.requireNonNull(template.insert(new Product(null, "coffee"))
                .flatMap(product -> template
                        .insert(new ProductPrice(null, product.getId(), BigDecimal.ONE, null))
                ).block()).getId();

        assertCount(Purchase.class, 0);

        Mono.just(new Purchase(1L, null, productPriceId, LocalDateTime.of(2024, 2, 20, 14, 59, 2)))
                .flatMap(persistence::insertPurchase)
                .as(StepVerifier::create)
                .verifyErrorSatisfies(error ->
                        assertThat(error.getMessage(), containsString("NULL not allowed for column \"USER_ID\""))
                )
        ;

        assertCount(Purchase.class, 0);
    }

    @Test
    void insertPurchase_negative_ProductIdNull() {

        final Long userId = Objects.requireNonNull(persistence.insertUser(
                new User(null, "", "", "")
        ).block()).getId();

        assertCount(Purchase.class, 0);

        Mono.just(new Purchase(1L, userId, null, LocalDateTime.of(2024, 2, 20, 14, 59, 2)))
                .flatMap(persistence::insertPurchase)
                .as(StepVerifier::create)
                .verifyErrorSatisfies(error ->
                        assertThat(error.getMessage(), containsString("NULL not allowed for column \"PRODUCT_PRICE_ID\""))
                )
        ;

        assertCount(Purchase.class, 0);
    }

    @ParameterizedTest
    @MethodSource
    void insertPurchase_negative_NullNotAllowed(final Purchase purchase, final String errorMessage) {

        final Long userId = Objects.requireNonNull(persistence.insertUser(
                new User(null, "", "", "")
        ).block()).getId();

        final Long productPriceId = Objects.requireNonNull(template.insert(new Product(null, "coffee"))
                .flatMap(product -> template
                        .insert(new ProductPrice(null, product.getId(), BigDecimal.ONE, null))
                ).block()).getId();

        purchase.setUserId(userId);
        purchase.setProductPriceId(productPriceId);

        assertCount(Purchase.class, 0);

        Mono.just(purchase)
                .flatMap(persistence::insertPurchase)
                .as(StepVerifier::create)
                .verifyErrorSatisfies(error ->
                        assertThat(error.getMessage(), containsString(errorMessage))
                )
        ;

        assertCount(Purchase.class, 0);
    }

    static Stream<Arguments> insertPurchase_negative_NullNotAllowed() {
        return Stream.of(
                Arguments.of(
                        new Purchase(null, null, null, null),
                        "NULL not allowed for column \"TIMESTAMP\""
                )
        );
    }

    @Test
    void insertPayment_positive() {

        final Long userId = Objects.requireNonNull(persistence.insertUser(
                new User(null, "", "", "")
        ).block()).getId();

        assertCount(Payment.class, 0);

        Mono.just(new Payment(null, userId, new BigDecimal("123.45"), LocalDateTime.of(2024, 2, 20, 14, 59, 2)))
                .flatMap(persistence::insertPayment)
                .as(StepVerifier::create)
                .assertNext(payment -> {
                    assertThat(payment.getId(), notNullValue());
                    assertThat(payment.getUserId(), is(userId));
                    assertThat(payment.getAmount(), is(new BigDecimal("123.45")));
                    assertThat(payment.getTimestamp(), is(LocalDateTime.of(2024, 2, 20, 14, 59, 2)));
                })
                .verifyComplete()
        ;

        assertCount(Payment.class, 1);
    }

    @Test
    void insertPayment_negative_DuplicatePrimaryKey() {

        final Long userId = Objects.requireNonNull(persistence.insertUser(
                new User(null, "", "", "")
        ).block()).getId();

        assertCount(Payment.class, 0);

        Mono.just(new Payment(null, userId, BigDecimal.ONE, LocalDateTime.now()))
                .flatMap(persistence::insertPayment)
                .flatMap(persistence::insertPayment)
                .as(StepVerifier::create)
                .verifyErrorSatisfies(error ->
                        assertThat(error.getMessage(), containsString("Unique index or primary key violation: \"PRIMARY KEY ON PUBLIC.PAYMENTS(ID)"))
                )
        ;

        assertCount(Payment.class, 1);
    }

    @Test
    void insertPayment_negative_UserIdNull() {

        assertCount(Payment.class, 0);

        Mono.just(new Payment(null, null, BigDecimal.ONE, LocalDateTime.now()))
                .flatMap(persistence::insertPayment)
                .as(StepVerifier::create)
                .verifyErrorSatisfies(error ->
                        assertThat(error.getMessage(), containsString("NULL not allowed for column \"USER_ID\""))
                )
        ;

        assertCount(Payment.class, 0);
    }

    @ParameterizedTest
    @MethodSource
    void insertPayment_negative_NullNotAllowed(final Payment payment, final String errorMessage) {

        final Long userId = Objects.requireNonNull(persistence.insertUser(
                new User(null, "", "", "")
        ).block()).getId();

        payment.setUserId(userId);

        assertCount(Payment.class, 0);

        Mono.just(payment)
                .flatMap(persistence::insertPayment)
                .as(StepVerifier::create)
                .verifyErrorSatisfies(error ->
                        assertThat(error.getMessage(), containsString(errorMessage))
                )
        ;

        assertCount(Purchase.class, 0);
    }

    static Stream<Arguments> insertPayment_negative_NullNotAllowed() {
        return Stream.of(
                Arguments.of(
                        new Payment(null, null, null, LocalDateTime.now()),
                        "NULL not allowed for column \"AMOUNT\""
                ),
                Arguments.of(
                        new Payment(null, null, BigDecimal.ONE, null),
                        "NULL not allowed for column \"TIMESTAMP\""
                )
        );
    }

    void assertCount(Class<?> clazz, final long count) {
        template.count(empty(), clazz)
                .as(StepVerifier::create)
                .expectNext(count)
                .verifyComplete()
        ;
    }
}

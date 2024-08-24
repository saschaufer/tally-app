package de.saschaufer.apps.tally.persistence;

import de.saschaufer.apps.tally.config.db.DbConfig;
import de.saschaufer.apps.tally.config.db.DbProperties;
import de.saschaufer.apps.tally.controller.dto.GetPaymentsResponse;
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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
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

        Mono.just(new User(null, "test-username@mail.com", "test-password", "test-role", "registration-secret", LocalDateTime.of(2024, 5, 19, 23, 54, 1), false))
                .flatMap(persistence::insertUser)
                .as(StepVerifier::create)
                .assertNext(user -> {
                    assertThat(user.getId(), notNullValue());
                    assertThat(user.getUsername(), is("test-username@mail.com"));
                    assertThat(user.getPassword(), is("test-password"));
                    assertThat(user.getRoles(), is("test-role"));
                    assertThat(user.getRegistrationSecret(), is("registration-secret"));
                    assertThat(user.getRegistrationOn(), is(LocalDateTime.of(2024, 5, 19, 23, 54, 1)));
                    assertThat(user.getRegistrationComplete(), is(false));
                })
                .verifyComplete()
        ;

        assertCount(User.class, 1);
    }

    @Test
    void insertUser_negative_DuplicatePrimaryKey() {

        assertCount(User.class, 0);

        Mono.just(new User(1L, "", "", "", "", LocalDateTime.now(), false))
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
                        new User(null, null, "", "", "", LocalDateTime.now(), true),
                        "NULL not allowed for column \"EMAIL\""
                ),
                Arguments.of(
                        new User(null, "", null, "", "", LocalDateTime.now(), true),
                        "NULL not allowed for column \"PASSWORD\""
                ),
                Arguments.of(
                        new User(null, "", "", null, "", LocalDateTime.now(), true),
                        "NULL not allowed for column \"ROLES\""
                ),
                Arguments.of(
                        new User(null, "", "", "", null, LocalDateTime.now(), true),
                        "NULL not allowed for column \"REGISTRATION_SECRET\""
                ),
                Arguments.of(
                        new User(null, "", "", "", "", null, true),
                        "NULL not allowed for column \"REGISTRATION_ON\""
                ),
                Arguments.of(
                        new User(null, "", "", "", "", LocalDateTime.now(), null),
                        "NULL not allowed for column \"REGISTRATION_COMPLETE\""
                )
        );
    }

    @Test
    void selectUser_positive_UserExists() {

        final String username = Objects.requireNonNull(persistence.insertUser(
                new User(null, "test-username@mail.com", "test-password", "test-role", "registration-secret", LocalDateTime.of(2024, 5, 19, 23, 54, 1), true)
        ).block()).getUsername();

        Mono.just(username)
                .flatMap(persistence::selectUser)
                .as(StepVerifier::create)
                .assertNext(user -> {
                    assertThat(user.getId(), notNullValue());
                    assertThat(user.getUsername(), is("test-username@mail.com"));
                    assertThat(user.getPassword(), is("test-password"));
                    assertThat(user.getRoles(), is("test-role"));
                    assertThat(user.getRegistrationSecret(), is("registration-secret"));
                    assertThat(user.getRegistrationOn(), is(LocalDateTime.of(2024, 5, 19, 23, 54, 1)));
                    assertThat(user.getRegistrationComplete(), is(true));
                })
                .verifyComplete();
    }

    @Test
    void selectUser_negative_UserNotExists() {

        Mono.just("username@mail.com")
                .flatMap(persistence::selectUser)
                .as(StepVerifier::create)
                .verifyErrorSatisfies(error ->
                        assertThat(error.getMessage(), containsString("User not found"))
                );
    }

    @Test
    void existsUser_positive_UserExists() {

        final String username = Objects.requireNonNull(persistence.insertUser(
                new User(null, "test-username@mail.com", "test-password", "test-role", "registration-secret", LocalDateTime.of(2024, 5, 19, 23, 54, 1), true)
        ).block()).getUsername();

        Mono.just(username)
                .flatMap(persistence::existsUser)
                .as(StepVerifier::create)
                .assertNext(exists -> assertThat(exists, is(true)))
                .verifyComplete();
    }

    @Test
    void existsUser_positive_UserNotExists() {

        Mono.just("username@mail.com")
                .flatMap(persistence::existsUser)
                .as(StepVerifier::create)
                .assertNext(exists -> assertThat(exists, is(false)))
                .verifyComplete();
    }

    @Test
    void updateUserRegistrationComplete_positive_OneUserUpdated() {

        Flux.just(
                        new User(null, "test-1@mail.com", "test-password", "test-role", "registration-secret", LocalDateTime.of(2024, 5, 19, 23, 54, 1), false),
                        new User(null, "test-2@mail.com", "test-password", "test-role", "registration-secret", LocalDateTime.of(2024, 5, 19, 23, 54, 1), false),
                        new User(null, "test-3@mail.com", "test-password", "test-role", "registration-secret", LocalDateTime.of(2024, 5, 19, 23, 54, 1), false)
                )
                .flatMap(persistence::insertUser)
                .subscribe();

        Mono.just("test-2@mail.com")
                .flatMap(persistence::updateUserRegistrationComplete)
                .as(StepVerifier::create)
                .expectNext()
                .verifyComplete();

        Mono.just("test-1@mail.com")
                .flatMap(persistence::selectUser)
                .as(StepVerifier::create)
                .assertNext(user -> assertThat(user.getRegistrationComplete(), is(false)))
                .verifyComplete();

        Mono.just("test-2@mail.com")
                .flatMap(persistence::selectUser)
                .as(StepVerifier::create)
                .assertNext(user -> assertThat(user.getRegistrationComplete(), is(true)))
                .verifyComplete();

        Mono.just("test-3@mail.com")
                .flatMap(persistence::selectUser)
                .as(StepVerifier::create)
                .assertNext(user -> assertThat(user.getRegistrationComplete(), is(false)))
                .verifyComplete();
    }

    @Test
    void updateUserRegistrationComplete_negative_NoUserUpdated() {

        Flux.just(
                        new User(null, "test-1@mail.com", "test-password", "test-role", "registration-secret", LocalDateTime.of(2024, 5, 19, 23, 54, 1), false),
                        new User(null, "test-2@mail.com", "test-password", "test-role", "registration-secret", LocalDateTime.of(2024, 5, 19, 23, 54, 1), false),
                        new User(null, "test-3@mail.com", "test-password", "test-role", "registration-secret", LocalDateTime.of(2024, 5, 19, 23, 54, 1), false)
                )
                .flatMap(persistence::insertUser)
                .subscribe();

        Mono.just("test-4@mail.com")
                .flatMap(persistence::updateUserRegistrationComplete)
                .as(StepVerifier::create)
                .verifyErrorSatisfies(error ->
                        assertThat(error.getMessage(), containsString("User not updated"))
                );

        Mono.just("test-1@mail.com")
                .flatMap(persistence::selectUser)
                .as(StepVerifier::create)
                .assertNext(user -> assertThat(user.getRegistrationComplete(), is(false)))
                .verifyComplete();

        Mono.just("test-2@mail.com")
                .flatMap(persistence::selectUser)
                .as(StepVerifier::create)
                .assertNext(user -> assertThat(user.getRegistrationComplete(), is(false)))
                .verifyComplete();

        Mono.just("test-3@mail.com")
                .flatMap(persistence::selectUser)
                .as(StepVerifier::create)
                .assertNext(user -> assertThat(user.getRegistrationComplete(), is(false)))
                .verifyComplete();
    }

    @Test
    void updateUserPassword_positive_UserExists() {

        final Long userId = Objects.requireNonNull(persistence.insertUser(
                new User(null, "test-username@mail.com", "test-password", "test-role", "registration-secret", LocalDateTime.of(2024, 5, 19, 23, 54, 1), true)
        ).block()).getId();

        Mono.just(userId)
                .flatMap(id -> persistence.updateUserPassword(id, "test-password-changed"))
                .as(StepVerifier::create)
                .expectNext()
                .verifyComplete();

        Mono.just("test-username@mail.com")
                .flatMap(persistence::selectUser)
                .as(StepVerifier::create)
                .assertNext(user -> {
                    assertThat(user.getId(), notNullValue());
                    assertThat(user.getUsername(), is("test-username@mail.com"));
                    assertThat(user.getPassword(), is("test-password-changed"));
                    assertThat(user.getRoles(), is("test-role"));
                    assertThat(user.getRegistrationSecret(), is("registration-secret"));
                    assertThat(user.getRegistrationOn(), is(LocalDateTime.of(2024, 5, 19, 23, 54, 1)));
                    assertThat(user.getRegistrationComplete(), is(true));
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
    void deleteUnregisteredUsers_positive_NoUserToDelete() {

        insertTestData();

        assertCount(User.class, 2);

        Mono.just(1L)
                .flatMap(id -> persistence.deleteUnregisteredUsers(LocalDateTime.of(2024, 5, 20, 0, 0, 0)))
                .as(StepVerifier::create)
                .expectNext().expectNext(0L)
                .verifyComplete();

        assertCount(User.class, 2);
    }

    @Test
    void deleteUnregisteredUsers_positive_TwoUsersToDelete() {

        insertTestData();

        Mono.just(1L)
                .flatMap(n -> persistence.insertUser(new User(null, "a", "", "", "", LocalDateTime.of(2024, 5, 19, 23, 54, 1), false)))
                .flatMap(n -> persistence.insertUser(new User(null, "b", "", "", "", LocalDateTime.of(2024, 5, 19, 23, 54, 1), false)))
                .flatMap(n -> persistence.insertUser(new User(null, "c", "", "", "", LocalDateTime.of(2024, 5, 21, 0, 0, 0), false)))
                .block();

        assertCount(User.class, 5);

        Mono.just(1L)
                .flatMap(id -> persistence.deleteUnregisteredUsers(LocalDateTime.of(2024, 5, 20, 0, 0, 0)))
                .as(StepVerifier::create)
                .expectNext().expectNext(2L)
                .verifyComplete();

        assertCount(User.class, 3);

        Flux.just("Alice@mail.com", "Bob@mail.com", "c")
                .flatMap(persistence::existsUser)
                .as(StepVerifier::create)
                .assertNext(exists -> assertThat(exists, is(true)))
                .assertNext(exists -> assertThat(exists, is(true)))
                .assertNext(exists -> assertThat(exists, is(true)))
                .verifyComplete();
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
    void selectProduct_positive_ProductExists() {

        final TestData testData = insertTestData();

        Mono.just(testData.product1.getId())
                .flatMap(persistence::selectProduct)
                .as(StepVerifier::create)
                .assertNext(product -> {
                    assertThat(product.getT1().getId(), is(testData.product1.getId()));
                    assertThat(product.getT1().getName(), is(testData.product1.getName()));
                    assertThat(product.getT2().getPrice(), is(testData.productPrice1.getPrice()));
                    assertThat(product.getT2().getValidUntil(), nullValue());
                })
                .verifyComplete();
    }

    @Test
    void selectProduct_negative_ProductNotExists() {

        final TestData testData = insertTestData();

        Mono.just(testData.product1.getId() - 1)
                .flatMap(persistence::selectProduct)
                .as(StepVerifier::create)
                .verifyErrorSatisfies(error ->
                        assertThat(error.getMessage(), containsString("Product not found"))
                );
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

    @Test
    void insertPurchase_positive() {

        final TestData testData = insertTestData();

        assertCount(Purchase.class, testData.numOfPurchases);

        persistence.insertPurchase(testData.user1.getId(), testData.product1.getId())
                .as(StepVerifier::create)
                .verifyComplete();

        assertCount(Purchase.class, testData.numOfPurchases + 1);
    }

    @Test
    void insertPurchase_negative_ProductPriceNotExist() {

        final TestData testData = insertTestData();

        assertCount(Purchase.class, testData.numOfPurchases);

        persistence.insertPurchase(testData.user1.getId(), testData.product3.getId())
                .as(StepVerifier::create)
                .verifyErrorSatisfies(error ->
                        assertThat(error.getMessage(), containsString("Product price not found"))
                );

        assertCount(Purchase.class, testData.numOfPurchases);
    }

    @Test
    void selectPurchases_positive() {

        final TestData testData = insertTestData();

        persistence.selectPurchases(testData.user1.getId())
                .as(StepVerifier::create)
                .assertNext(purchases -> {
                    assertThat(purchases.size(), is(3));

                    assertThat(purchases.getFirst().purchaseId(), is(testData.purchase1.getId()));
                    assertThat(purchases.getFirst().purchaseTimestamp(), is(testData.purchase1.getTimestamp()));
                    assertThat(purchases.getFirst().productName(), is(testData.product1.getName()));
                    assertThat(purchases.getFirst().productPrice(), is(testData.productPrice1.getPrice()));

                    assertThat(purchases.get(1).purchaseId(), is(testData.purchase3.getId()));
                    assertThat(purchases.get(1).purchaseTimestamp(), is(testData.purchase3.getTimestamp()));
                    assertThat(purchases.get(1).productName(), is(testData.product1.getName()));
                    assertThat(purchases.get(1).productPrice(), is(testData.productPrice1.getPrice()));

                    assertThat(purchases.getLast().purchaseId(), is(testData.purchase2.getId()));
                    assertThat(purchases.getLast().purchaseTimestamp(), is(testData.purchase2.getTimestamp()));
                    assertThat(purchases.getLast().productName(), is(testData.product2.getName()));
                    assertThat(purchases.getLast().productPrice(), is(testData.productPrice5.getPrice()));
                })
                .verifyComplete();
    }

    @Test
    void selectPurchasesSum_positive() {

        final TestData testData = insertTestData();

        persistence.selectPurchasesSum(testData.user1.getId())
                .as(StepVerifier::create)
                .assertNext(sum -> assertThat(sum, is(testData.productPrice1.getPrice().multiply(BigDecimal.TWO).add(testData.productPrice5.getPrice()))))
                .verifyComplete();
    }

    @Test
    void selectPurchasesSum_positive_NoPayments() {

        final TestData testData = insertTestData();

        persistence.selectPurchasesSum(testData.user1.getId() - 1)
                .as(StepVerifier::create)
                .assertNext(sum -> assertThat(sum, is(BigDecimal.ZERO)))
                .verifyComplete();
    }

    @Test
    void deletePurchase_positive() {

        final TestData testData = insertTestData();

        assertCount(Purchase.class, testData.numOfPurchases);

        persistence.deletePurchase(testData.purchase1.getId())
                .as(StepVerifier::create)
                .verifyComplete();

        assertCount(Purchase.class, testData.numOfPurchases - 1);
    }

    @Test
    void deletePurchase_negative_PurchaseIdNotExist() {

        final TestData testData = insertTestData();

        assertCount(Purchase.class, testData.numOfPurchases);

        persistence.deletePurchase(testData.purchase1.getId() - 1)
                .as(StepVerifier::create)
                .verifyErrorSatisfies(error ->
                        assertThat(error.getMessage(), containsString("Purchase not deleted"))
                );

        assertCount(Purchase.class, testData.numOfPurchases);
    }

    @Test
    void insertPayment_positive() {

        final Long userId = Objects.requireNonNull(persistence.insertUser(
                new User(null, "", "", "", "", LocalDateTime.now(), true)
        ).block()).getId();

        assertCount(Payment.class, 0);

        Mono.just(new Payment(null, userId, new BigDecimal("123.45"), LocalDateTime.of(2024, 2, 20, 14, 59, 2)))
                .flatMap(persistence::insertPayment)
                .then(Mono.fromCallable(() -> userId))
                .flatMap(persistence::selectPayments)
                .as(StepVerifier::create)
                .assertNext(payments -> {

                    assertThat(payments.size(), is(1));

                    final GetPaymentsResponse payment = payments.getFirst();

                    assertThat(payment.id(), notNullValue());
                    assertThat(payment.amount(), is(new BigDecimal("123.45")));
                    assertThat(payment.timestamp(), is(LocalDateTime.of(2024, 2, 20, 14, 59, 2)));
                })
                .verifyComplete()
        ;

        assertCount(Payment.class, 1);
    }

    @Test
    void insertPayment_negative_DuplicatePrimaryKey() {

        final Long userId = Objects.requireNonNull(persistence.insertUser(
                new User(null, "", "", "", "", LocalDateTime.now(), true)
        ).block()).getId();

        assertCount(Payment.class, 0);

        Mono.just(new Payment(null, userId, BigDecimal.ONE, LocalDateTime.now()))
                .flatMap(persistence::insertPayment)
                .then(Mono.defer(() -> persistence.selectPayments(userId).map(List::getFirst)))
                .map(p -> new Payment(p.id(), userId, p.amount(), p.timestamp()))
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
                new User(null, "", "", "", "", LocalDateTime.now(), true)
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

    @Test
    void selectPayments_positive() {

        final TestData testData = insertTestData();

        persistence.selectPayments(testData.user1.getId())
                .as(StepVerifier::create)
                .assertNext(list -> {

                    assertThat(list.size(), is(2));

                    assertThat(list.getFirst().id(), is(testData.payment1.getId()));
                    assertThat(list.getFirst().amount(), is(testData.payment1.getAmount()));
                    assertThat(list.getFirst().timestamp(), is(testData.payment1.getTimestamp()));

                    assertThat(list.getLast().id(), is(testData.payment2.getId()));
                    assertThat(list.getLast().amount(), is(testData.payment2.getAmount()));
                    assertThat(list.getLast().timestamp(), is(testData.payment2.getTimestamp()));

                })
                .verifyComplete();
    }

    @Test
    void selectPayments_positive_NoPayments() {

        final TestData testData = insertTestData();

        persistence.selectPayments(testData.user1.getId() - 1)
                .as(StepVerifier::create)
                .assertNext(list -> assertThat(list.size(), is(0)))
                .verifyComplete();
    }

    @Test
    void selectPaymentsSum_positive() {

        final TestData testData = insertTestData();

        persistence.selectPaymentsSum(testData.user1.getId())
                .as(StepVerifier::create)
                .assertNext(sum -> assertThat(sum, is(testData.payment1.getAmount().add(testData.payment2.getAmount()))))
                .verifyComplete();
    }

    @Test
    void selectPaymentsSum_positive_NoPayments() {

        final TestData testData = insertTestData();

        persistence.selectPaymentsSum(testData.user1.getId() - 1)
                .as(StepVerifier::create)
                .assertNext(sum -> assertThat(sum, is(BigDecimal.ZERO)))
                .verifyComplete();
    }

    @Test
    void deletePayment_positive() {

        final TestData testData = insertTestData();

        assertCount(Payment.class, testData.numOfPayments);

        persistence.deletePayment(testData.payment1.getId())
                .as(StepVerifier::create)
                .verifyComplete();

        assertCount(Payment.class, testData.numOfPayments - 1);
    }

    @Test
    void deletePayment_negative_PaymentIdNotExist() {

        final TestData testData = insertTestData();

        assertCount(Payment.class, testData.numOfPayments);

        persistence.deletePayment(testData.payment1.getId() - 1)
                .as(StepVerifier::create)
                .verifyErrorSatisfies(error ->
                        assertThat(error.getMessage(), containsString("Payment not deleted"))
                );

        assertCount(Payment.class, testData.numOfPayments);
    }

    void assertCount(Class<?> clazz, final long count) {
        template.count(empty(), clazz)
                .as(StepVerifier::create)
                .expectNext(count)
                .verifyComplete()
        ;
    }

    private TestData insertTestData() {

        final TestData testData = new TestData();

        testData.numOfUsers = 2;
        testData.user1 = template.insert(new User(null, "Alice@mail.com", "password", "roles", "11111", LocalDateTime.of(2024, 5, 19, 23, 54, 1), true)).block();
        testData.user2 = template.insert(new User(null, "Bob@mail.com", "password", "roles", "22222", LocalDateTime.of(2024, 5, 19, 23, 54, 1), true)).block();

        testData.numOfProducts = 2;
        testData.product1 = template.insert(new Product(null, "Coffee")).block();
        testData.product2 = template.insert(new Product(null, "Tea")).block();
        testData.product3 = template.insert(new Product(null, "Soda")).block();

        testData.numOfProductPrices = 7;
        testData.productPrice1 = template.insert(new ProductPrice(null, testData.product1.getId(), new BigDecimal("0.30"), null)).block();
        testData.productPrice2 = template.insert(new ProductPrice(null, testData.product1.getId(), new BigDecimal("0.20"), LocalDateTime.of(2024, 5, 19, 23, 54, 1))).block();
        testData.productPrice3 = template.insert(new ProductPrice(null, testData.product1.getId(), new BigDecimal("0.10"), LocalDateTime.of(2024, 5, 20, 10, 23, 45))).block();

        testData.productPrice4 = template.insert(new ProductPrice(null, testData.product2.getId(), new BigDecimal("0.50"), null)).block();
        testData.productPrice5 = template.insert(new ProductPrice(null, testData.product2.getId(), new BigDecimal("0.45"), LocalDateTime.of(2024, 5, 19, 13, 41, 53))).block();
        testData.productPrice6 = template.insert(new ProductPrice(null, testData.product2.getId(), new BigDecimal("0.40"), LocalDateTime.of(2024, 5, 20, 21, 32, 24))).block();

        testData.productPrice7 = template.insert(new ProductPrice(null, testData.product3.getId(), new BigDecimal("0.60"), LocalDateTime.of(2024, 5, 20, 15, 10, 56))).block();

        testData.numOfPurchases = 6;
        testData.purchase1 = template.insert(new Purchase(null, testData.user1.getId(), testData.productPrice1.getId(), LocalDateTime.of(2024, 5, 23, 12, 45, 31))).block();
        testData.purchase2 = template.insert(new Purchase(null, testData.user1.getId(), testData.productPrice5.getId(), LocalDateTime.of(2024, 5, 23, 16, 32, 53))).block();
        testData.purchase3 = template.insert(new Purchase(null, testData.user1.getId(), testData.productPrice1.getId(), LocalDateTime.of(2024, 5, 23, 21, 51, 13))).block();

        testData.purchase4 = template.insert(new Purchase(null, testData.user2.getId(), testData.productPrice4.getId(), LocalDateTime.of(2024, 5, 23, 18, 13, 53))).block();
        testData.purchase5 = template.insert(new Purchase(null, testData.user2.getId(), testData.productPrice1.getId(), LocalDateTime.of(2024, 5, 23, 17, 24, 17))).block();
        testData.purchase6 = template.insert(new Purchase(null, testData.user2.getId(), testData.productPrice2.getId(), LocalDateTime.of(2024, 5, 23, 12, 36, 24))).block();

        testData.numOfPayments = 3;
        testData.payment1 = template.insert(new Payment(null, testData.user1.getId(), new BigDecimal("1.87"), LocalDateTime.of(2024, 5, 23, 12, 45, 31))).block();
        testData.payment2 = template.insert(new Payment(null, testData.user1.getId(), new BigDecimal("5.54"), LocalDateTime.of(2024, 5, 23, 12, 36, 24))).block();

        testData.payment3 = template.insert(new Payment(null, testData.user2.getId(), new BigDecimal("2.45"), LocalDateTime.of(2024, 5, 23, 17, 24, 17))).block();

        return testData;
    }

    private class TestData {

        Integer numOfUsers;
        Integer numOfProducts;
        Integer numOfProductPrices;
        Integer numOfPurchases;
        Integer numOfPayments;

        User user1;
        User user2;

        Product product1;
        Product product2;
        Product product3;

        ProductPrice productPrice1;
        ProductPrice productPrice2;
        ProductPrice productPrice3;

        ProductPrice productPrice4;
        ProductPrice productPrice5;
        ProductPrice productPrice6;

        ProductPrice productPrice7;

        Purchase purchase1;
        Purchase purchase2;
        Purchase purchase3;

        Purchase purchase4;
        Purchase purchase6;
        Purchase purchase5;

        Payment payment1;
        Payment payment2;

        Payment payment3;
        Payment payment4;
    }
}

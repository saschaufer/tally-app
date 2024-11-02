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
import reactor.util.function.Tuple2;

import java.math.BigDecimal;
import java.time.Instant;
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

        Mono.just(new User(null, "test-username@mail.com", "test-password", "test-role", "registration-secret", Instant.parse("2024-05-19T23:54:01Z"), false))
                .flatMap(persistence::insertUser)
                .as(StepVerifier::create)
                .assertNext(user -> {
                    assertThat(user.getId(), notNullValue());
                    assertThat(user.getUsername(), is("test-username@mail.com"));
                    assertThat(user.getPassword(), is("test-password"));
                    assertThat(user.getRoles(), is("test-role"));
                    assertThat(user.getRegistrationSecret(), is("registration-secret"));
                    assertThat(user.getRegistrationOn(), is(Instant.parse("2024-05-19T23:54:01Z")));
                    assertThat(user.getRegistrationComplete(), is(false));
                })
                .verifyComplete()
        ;

        assertCount(User.class, 1);
    }

    @Test
    void insertUser_negative_DuplicatePrimaryKey() {

        assertCount(User.class, 0);

        Mono.just(new User(1L, "", "", "", "", Instant.now(), false))
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
                        new User(null, null, "", "", "", Instant.now(), true),
                        "NULL not allowed for column \"EMAIL\""
                ),
                Arguments.of(
                        new User(null, "", null, "", "", Instant.now(), true),
                        "NULL not allowed for column \"PASSWORD\""
                ),
                Arguments.of(
                        new User(null, "", "", null, "", Instant.now(), true),
                        "NULL not allowed for column \"ROLES\""
                ),
                Arguments.of(
                        new User(null, "", "", "", null, Instant.now(), true),
                        "NULL not allowed for column \"REGISTRATION_SECRET\""
                ),
                Arguments.of(
                        new User(null, "", "", "", "", null, true),
                        "NULL not allowed for column \"REGISTRATION_ON\""
                ),
                Arguments.of(
                        new User(null, "", "", "", "", Instant.now(), null),
                        "NULL not allowed for column \"REGISTRATION_COMPLETE\""
                )
        );
    }

    @Test
    void selectUser_positive_UserExists() {

        final String username = Objects.requireNonNull(persistence.insertUser(
                new User(null, "test-username@mail.com", "test-password", "test-role", "registration-secret", Instant.parse("2024-05-19T23:54:01Z"), true)
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
                    assertThat(user.getRegistrationOn(), is(Instant.parse("2024-05-19T23:54:01Z")));
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
    void selectUsers_positive_UsersExist() {

        final TestData testData = insertTestData();

        assertCount(User.class, 2);

        persistence.selectUsers()
                .as(StepVerifier::create)
                .assertNext(users -> {

                    assertThat(users.size(), is(testData.numOfUsers));

                    assertThat(users.getFirst().getId(), notNullValue());
                    assertThat(users.getFirst().getUsername(), is(testData.user1.getEmail()));
                    assertThat(users.getFirst().getPassword(), nullValue());
                    assertThat(users.getFirst().getRoles(), is(testData.user1.getRoles()));
                    assertThat(users.getFirst().getRegistrationSecret(), nullValue());
                    assertThat(users.getFirst().getRegistrationOn(), is(testData.user1.getRegistrationOn()));
                    assertThat(users.getFirst().getRegistrationComplete(), is(testData.user1.getRegistrationComplete()));

                    assertThat(users.getLast().getId(), notNullValue());
                    assertThat(users.getLast().getUsername(), is(testData.user2.getEmail()));
                    assertThat(users.getLast().getPassword(), nullValue());
                    assertThat(users.getLast().getRoles(), is(testData.user2.getRoles()));
                    assertThat(users.getLast().getRegistrationSecret(), nullValue());
                    assertThat(users.getLast().getRegistrationOn(), is(testData.user2.getRegistrationOn()));
                    assertThat(users.getLast().getRegistrationComplete(), is(testData.user2.getRegistrationComplete()));
                })
                .verifyComplete();
    }

    @Test
    void selectUsers_positive_UsersNotExist() {

        persistence.selectUsers()
                .as(StepVerifier::create)
                .assertNext(users -> assertThat(users.size(), is(0)))
                .verifyComplete();
    }

    @Test
    void existsUser_positive_UserExists() {

        final String username = Objects.requireNonNull(persistence.insertUser(
                new User(null, "test-username@mail.com", "test-password", "test-role", "registration-secret", Instant.parse("2024-05-19T23:54:01Z"), true)
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
    void deleteUser_positive_UserExists() {

        final TestData testData = insertTestData();

        assertCount(User.class, testData.numOfUsers);
        assertCount(Purchase.class, testData.numOfPurchases);
        assertCount(Payment.class, testData.numOfPayments);

        Mono.just(testData.user2.getId())
                .flatMap(persistence::deleteUser)
                .as(StepVerifier::create)
                .expectNext()
                .verifyComplete();

        assertCount(User.class, testData.numOfUsers - 1);
        assertCount(Purchase.class, testData.numOfPurchases - 3);
        assertCount(Payment.class, testData.numOfPayments - 1);

        persistence.existsUser(testData.user2.getEmail())
                .as(StepVerifier::create)
                .assertNext(b -> assertThat(b, is(false)))
                .verifyComplete();
    }

    @Test
    void deleteUser_negative_rollback_DeleteUserFailed() {

        template.getDatabaseClient().sql("set referential_integrity false").then().block();

        Mono.just(1)
                .flatMap(i -> template.insert(new Purchase(null, 2L, 3L, Instant.now())))
                .flatMap(i -> template.insert(new Payment(null, 2L, BigDecimal.ONE, Instant.now())))
                .flatMap(i -> template.insert(new User(1L, "1@mail", "pwd", "role", "123", Instant.now(), true)))
                .block();

        template.getDatabaseClient().sql("set referential_integrity true").then().block();

        assertCount(Purchase.class, 1);
        assertCount(Payment.class, 1);
        assertCount(User.class, 1);

        Mono.just(2L)
                .flatMap(persistence::deleteUser)
                .as(StepVerifier::create)
                .verifyErrorSatisfies(error ->
                        assertThat(error.getMessage(), containsString("User not deleted"))
                );

        assertCount(Purchase.class, 1);
        assertCount(Payment.class, 1);
        assertCount(User.class, 1);
    }

    @Test
    void updateUserRegistrationComplete_positive_OneUserUpdated() {

        Flux.just(
                        new User(null, "test-1@mail.com", "test-password", "test-role", "registration-secret", Instant.parse("2024-05-19T23:54:01Z"), false),
                        new User(null, "test-2@mail.com", "test-password", "test-role", "registration-secret", Instant.parse("2024-05-19T23:54:01Z"), false),
                        new User(null, "test-3@mail.com", "test-password", "test-role", "registration-secret", Instant.parse("2024-05-19T23:54:01Z"), false)
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
                        new User(null, "test-1@mail.com", "test-password", "test-role", "registration-secret", Instant.parse("2024-05-19T23:54:01Z"), false),
                        new User(null, "test-2@mail.com", "test-password", "test-role", "registration-secret", Instant.parse("2024-05-19T23:54:01Z"), false),
                        new User(null, "test-3@mail.com", "test-password", "test-role", "registration-secret", Instant.parse("2024-05-19T23:54:01Z"), false)
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
                new User(null, "test-username@mail.com", "test-password", "test-role", "registration-secret", Instant.parse("2024-05-19T23:54:01Z"), true)
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
                    assertThat(user.getRegistrationOn(), is(Instant.parse("2024-05-19T23:54:01Z")));
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
                .flatMap(id -> persistence.deleteUnregisteredUsers(Instant.parse("2024-05-20T00:00:00Z")))
                .as(StepVerifier::create)
                .expectNext().expectNext(0L)
                .verifyComplete();

        assertCount(User.class, 2);
    }

    @Test
    void deleteUnregisteredUsers_positive_TwoUsersToDelete() {

        insertTestData();

        Mono.just(1L)
                .flatMap(n -> persistence.insertUser(new User(null, "a", "", "", "", Instant.parse("2024-05-19T23:54:01Z"), false)))
                .flatMap(n -> persistence.insertUser(new User(null, "b", "", "", "", Instant.parse("2024-05-19T23:54:01Z"), false)))
                .flatMap(n -> persistence.insertUser(new User(null, "c", "", "", "", Instant.parse("2024-05-21T00:00:00Z"), false)))
                .block();

        assertCount(User.class, 5);

        Mono.just(1L)
                .flatMap(id -> persistence.deleteUnregisteredUsers(Instant.parse("2024-05-20T00:00:00Z")))
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
    void selectProduct_ByName_positive_ProductExists() {

        final TestData testData = insertTestData();

        Mono.just(testData.product1.getName())
                .flatMap(persistence::selectProduct)
                .as(StepVerifier::create)
                .assertNext(product -> {
                    assertThat(product.getId(), is(testData.product1.getId()));
                    assertThat(product.getName(), is(testData.product1.getName()));
                })
                .verifyComplete();
    }

    @Test
    void selectProduct_ByName_negative_ProductNotExists() {

        final TestData testData = insertTestData();

        Mono.just(testData.product1.getName() + "Not")
                .flatMap(persistence::selectProduct)
                .as(StepVerifier::create)
                .verifyErrorSatisfies(error ->
                        assertThat(error.getMessage(), containsString("Product not found"))
                );
    }

    @Test
    void selectProduct_ById_positive_ProductExists() {

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
    void selectProduct_ById_negative_ProductNotExists() {

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
                                .then(template.insert(new ProductPrice(null, product.getId(), new BigDecimal("0.2"), Instant.now())))
                                .then(template.insert(new ProductPrice(null, product.getId(), new BigDecimal("0.3"), Instant.now())))
                )
                .flatMap(p -> Mono.empty());

        final Mono<Void> tea = template.insert(new Product(null, "tea"))
                .flatMap(product ->
                        template.insert(new ProductPrice(null, product.getId(), new BigDecimal("0.4"), null))
                                .then(template.insert(new ProductPrice(null, product.getId(), new BigDecimal("0.5"), Instant.now())))
                                .then(template.insert(new ProductPrice(null, product.getId(), new BigDecimal("0.6"), Instant.now())))
                )
                .flatMap(p -> Mono.empty());

        final Mono<Void> hotChocolate = template.insert(new Product(null, "hot chocolate"))
                .flatMap(product ->
                        template.insert(new ProductPrice(null, product.getId(), new BigDecimal("0.7"), Instant.now()))
                                .then(template.insert(new ProductPrice(null, product.getId(), new BigDecimal("0.8"), Instant.now())))
                                .then(template.insert(new ProductPrice(null, product.getId(), new BigDecimal("0.9"), Instant.now())))
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
    void existsProduct_positive_ProductExists() {

        final TestData testData = insertTestData();

        Mono.just(testData.product1.getName())
                .flatMap(persistence::existsProduct)
                .as(StepVerifier::create)
                .assertNext(exists -> assertThat(exists, is(true)))
                .verifyComplete();
    }

    @Test
    void existsProduct_positive_ProductNotExists() {

        final TestData testData = insertTestData();

        Mono.just(testData.product1.getName() + "NOT")
                .flatMap(persistence::existsProduct)
                .as(StepVerifier::create)
                .assertNext(exists -> assertThat(exists, is(false)))
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
    void deleteProduct_positive() {

        final TestData testData = insertTestData();

        persistence.selectProducts()
                .as(StepVerifier::create)
                .assertNext(products -> {
                    assertThat(products.size(), is(testData.numOfProducts));
                    final List<Tuple2<Product, ProductPrice>> products2 = products.stream().filter(p -> p.getT1().getName().equals(testData.product2.getName())).toList();
                    assertThat(products2.size(), is(1));
                    assertThat(products2.getFirst().getT1().getName(), is(testData.product2.getName()));
                })
                .verifyComplete();

        persistence.deleteProduct(testData.product2.getId())
                .as(StepVerifier::create)
                .verifyComplete();

        persistence.selectProducts()
                .as(StepVerifier::create)
                .assertNext(products -> {
                    assertThat(products.size(), is(testData.numOfProducts - 1));
                    final List<Tuple2<Product, ProductPrice>> products2 = products.stream().filter(p -> p.getT1().getName().equals(testData.product2.getName())).toList();
                    assertThat(products2.size(), is(0));
                })
                .verifyComplete();
    }

    @Test
    void deleteProduct_negative_ProductNotExist() {

        persistence.deleteProduct(1L)
                .as(StepVerifier::create)
                .verifyErrorSatisfies(error ->
                        assertThat(error.getMessage(), containsString("Product not deleted"))
                );
    }

    @Test
    void insertProductPrice_positive() {


        final Product product = template.insert(new Product(null, "coffee")).block();

        assertCount(ProductPrice.class, 0);

        persistence.insertProductPrice(product.getId(), new BigDecimal("123.45"))
                .as(StepVerifier::create)
                .assertNext(productPrice -> {
                    assertThat(productPrice.getId(), notNullValue());
                    assertThat(productPrice.getProductId(), is(product.getId()));
                    assertThat(productPrice.getPrice(), is(new BigDecimal("123.45")));
                    assertThat(productPrice.getValidUntil(), nullValue());
                })
                .verifyComplete();

        assertCount(ProductPrice.class, 1);
    }

    @Test
    void insertProductPrice_negative_ProductNotExist() {

        assertCount(ProductPrice.class, 0);

        persistence.insertProductPrice(1L, new BigDecimal("123.45"))
                .as(StepVerifier::create)
                .verifyErrorSatisfies(error ->
                        assertThat(error.getMessage(), containsString("Referential integrity constraint violation"))
                );

        assertCount(ProductPrice.class, 0);
    }

    @Test
    void existsProductPrice_positive_ProductPriceExists() {

        final TestData testData = insertTestData();

        Mono.just(testData.product1.getId())
                .flatMap(persistence::existsProductPrice)
                .as(StepVerifier::create)
                .assertNext(exists -> assertThat(exists, is(true)))
                .verifyComplete();
    }

    @Test
    void existsProductPrice_negative_ProductPriceNotExists() {

        final TestData testData = insertTestData();

        Mono.just(testData.product1.getId())
                .flatMap(persistence::deleteProduct)
                .as(StepVerifier::create)
                .verifyComplete();

        Mono.just(testData.product1.getId())
                .flatMap(persistence::existsProductPrice)
                .as(StepVerifier::create)
                .assertNext(exists -> assertThat(exists, is(false)))
                .verifyComplete();
    }

    @Test
    void updateProductPrice_positive_PriceExists() {

        final Long p1Id = Objects.requireNonNull(template.insert(new Product(null, "coffee"))
                .flatMap(product ->
                        template.insert(new ProductPrice(null, product.getId(), new BigDecimal("0.1"), null))
                                .then(template.insert(new ProductPrice(null, product.getId(), new BigDecimal("0.2"), Instant.now())))
                                .then(template.insert(new ProductPrice(null, product.getId(), new BigDecimal("0.3"), Instant.now())))
                                .then(Mono.just(product))
                ).block()).getId();

        template.insert(new Product(null, "tea"))
                .flatMap(product ->
                        template.insert(new ProductPrice(null, product.getId(), new BigDecimal("0.4"), null))
                                .then(template.insert(new ProductPrice(null, product.getId(), new BigDecimal("0.5"), Instant.now())))
                                .then(template.insert(new ProductPrice(null, product.getId(), new BigDecimal("0.6"), Instant.now())))
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
                        template.insert(new ProductPrice(null, product.getId(), new BigDecimal("0.1"), Instant.now()))
                                .then(template.insert(new ProductPrice(null, product.getId(), new BigDecimal("0.2"), Instant.now())))
                                .then(template.insert(new ProductPrice(null, product.getId(), new BigDecimal("0.3"), Instant.now())))
                                .then(Mono.just(product))
                ).block()).getId();

        template.insert(new Product(null, "tea"))
                .flatMap(product ->
                        template.insert(new ProductPrice(null, product.getId(), new BigDecimal("0.4"), null))
                                .then(template.insert(new ProductPrice(null, product.getId(), new BigDecimal("0.5"), Instant.now())))
                                .then(template.insert(new ProductPrice(null, product.getId(), new BigDecimal("0.6"), Instant.now())))
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
                                .then(template.insert(new ProductPrice(null, product.getId(), new BigDecimal("0.2"), Instant.now())))
                                .then(template.insert(new ProductPrice(null, product.getId(), new BigDecimal("0.3"), Instant.now())))
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
    void selectPurchasesSumAllUsers_positive() {

        final TestData testData = insertTestData();

        persistence.selectPurchasesSumAllUsers()
                .as(StepVerifier::create)
                .assertNext(sums -> {
                    assertThat(sums.size(), is(testData.numOfUsers));
                    assertThat(sums.get(testData.user1.getId()), is(testData.productPrice1.getPrice().multiply(BigDecimal.TWO).add(testData.productPrice5.getPrice())));
                    assertThat(sums.get(testData.user2.getId()), is(testData.productPrice1.getPrice().add(testData.productPrice2.getPrice()).add(testData.productPrice4.getPrice())));
                })
                .verifyComplete();
    }

    @Test
    void selectPurchasesSumAllUsers_positive_NoPurchases() {

        persistence.selectPurchasesSumAllUsers()
                .as(StepVerifier::create)
                .assertNext(sums -> assertThat(sums.size(), is(0)))
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
                new User(null, "", "", "", "", Instant.now(), true)
        ).block()).getId();

        assertCount(Payment.class, 0);

        Mono.just(new Payment(null, userId, new BigDecimal("123.45"), Instant.parse("2024-02-20T14:59:02Z")))
                .flatMap(persistence::insertPayment)
                .then(Mono.fromCallable(() -> userId))
                .flatMap(persistence::selectPayments)
                .as(StepVerifier::create)
                .assertNext(payments -> {

                    assertThat(payments.size(), is(1));

                    final GetPaymentsResponse payment = payments.getFirst();

                    assertThat(payment.id(), notNullValue());
                    assertThat(payment.amount(), is(new BigDecimal("123.45")));
                    assertThat(payment.timestamp(), is(Instant.parse("2024-02-20T14:59:02Z")));
                })
                .verifyComplete()
        ;

        assertCount(Payment.class, 1);
    }

    @Test
    void insertPayment_negative_DuplicatePrimaryKey() {

        final Long userId = Objects.requireNonNull(persistence.insertUser(
                new User(null, "", "", "", "", Instant.now(), true)
        ).block()).getId();

        assertCount(Payment.class, 0);

        Mono.just(new Payment(null, userId, BigDecimal.ONE, Instant.now()))
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

        Mono.just(new Payment(null, null, BigDecimal.ONE, Instant.now()))
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
                new User(null, "", "", "", "", Instant.now(), true)
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
                        new Payment(null, null, null, Instant.now()),
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
    void selectPaymentsSumAllUsers_positive() {

        final TestData testData = insertTestData();

        persistence.selectPaymentsSumAllUsers()
                .as(StepVerifier::create)
                .assertNext(sums -> {
                    assertThat(sums.size(), is(testData.numOfUsers));
                    assertThat(sums.get(testData.user1.getId()), is(testData.payment1.getAmount().add(testData.payment2.getAmount())));
                    assertThat(sums.get(testData.user2.getId()), is(testData.payment3.getAmount()));
                })
                .verifyComplete();
    }

    @Test
    void selectPaymentsSumAllUsers_positive_NoPayments() {

        persistence.selectPaymentsSumAllUsers()
                .as(StepVerifier::create)
                .assertNext(sums -> assertThat(sums.size(), is(0)))
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
        testData.user1 = template.insert(new User(null, "Alice@mail.com", "password", "roles", "11111", Instant.parse("2024-05-19T23:54:01Z"), true)).block();
        testData.user2 = template.insert(new User(null, "Bob@mail.com", "password", "roles", "22222", Instant.parse("2024-05-19T23:54:01Z"), true)).block();

        testData.numOfProducts = 2;
        testData.product1 = template.insert(new Product(null, "Coffee")).block();
        testData.product2 = template.insert(new Product(null, "Tea")).block();
        testData.product3 = template.insert(new Product(null, "Soda")).block();

        testData.numOfProductPrices = 7;
        testData.productPrice1 = template.insert(new ProductPrice(null, testData.product1.getId(), new BigDecimal("0.30"), null)).block();
        testData.productPrice2 = template.insert(new ProductPrice(null, testData.product1.getId(), new BigDecimal("0.20"), Instant.parse("2024-05-19T23:54:01Z"))).block();
        testData.productPrice3 = template.insert(new ProductPrice(null, testData.product1.getId(), new BigDecimal("0.10"), Instant.parse("2024-05-20T10:23:45Z"))).block();

        testData.productPrice4 = template.insert(new ProductPrice(null, testData.product2.getId(), new BigDecimal("0.50"), null)).block();
        testData.productPrice5 = template.insert(new ProductPrice(null, testData.product2.getId(), new BigDecimal("0.45"), Instant.parse("2024-05-19T13:41:53Z"))).block();
        testData.productPrice6 = template.insert(new ProductPrice(null, testData.product2.getId(), new BigDecimal("0.40"), Instant.parse("2024-05-20T21:32:24Z"))).block();

        testData.productPrice7 = template.insert(new ProductPrice(null, testData.product3.getId(), new BigDecimal("0.60"), Instant.parse("2024-05-20T15:10:56Z"))).block();

        testData.numOfPurchases = 6;
        testData.purchase1 = template.insert(new Purchase(null, testData.user1.getId(), testData.productPrice1.getId(), Instant.parse("2024-05-23T12:45:31Z"))).block();
        testData.purchase2 = template.insert(new Purchase(null, testData.user1.getId(), testData.productPrice5.getId(), Instant.parse("2024-05-23T16:32:53Z"))).block();
        testData.purchase3 = template.insert(new Purchase(null, testData.user1.getId(), testData.productPrice1.getId(), Instant.parse("2024-05-23T21:51:13Z"))).block();

        testData.purchase4 = template.insert(new Purchase(null, testData.user2.getId(), testData.productPrice4.getId(), Instant.parse("2024-05-23T18:13:53Z"))).block();
        testData.purchase5 = template.insert(new Purchase(null, testData.user2.getId(), testData.productPrice1.getId(), Instant.parse("2024-05-23T17:24:17Z"))).block();
        testData.purchase6 = template.insert(new Purchase(null, testData.user2.getId(), testData.productPrice2.getId(), Instant.parse("2024-05-23T12:36:24Z"))).block();

        testData.numOfPayments = 3;
        testData.payment1 = template.insert(new Payment(null, testData.user1.getId(), new BigDecimal("1.87"), Instant.parse("2024-05-23T12:45:31Z"))).block();
        testData.payment2 = template.insert(new Payment(null, testData.user1.getId(), new BigDecimal("5.54"), Instant.parse("2024-05-23T12:36:24Z"))).block();

        testData.payment3 = template.insert(new Payment(null, testData.user2.getId(), new BigDecimal("2.45"), Instant.parse("2024-05-23T17:24:17Z"))).block();

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

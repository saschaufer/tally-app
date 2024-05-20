package de.saschaufer.apps.tally.services;

import de.saschaufer.apps.tally.persistence.Persistence;
import de.saschaufer.apps.tally.persistence.dto.Product;
import de.saschaufer.apps.tally.persistence.dto.ProductPrice;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.util.function.Tuples;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ProductServiceTest {

    private Persistence persistence;
    private ProductService productService;

    @BeforeEach
    void beforeEach() {
        persistence = mock(Persistence.class);
        productService = new ProductService(persistence);
    }

    @Test
    void createProduct_positive() {

        doReturn(Mono.empty()).when(persistence).insertProductAndPrice(any(String.class), any(BigDecimal.class));

        productService.createProduct("test-name", BigDecimal.ONE)
                .as(StepVerifier::create)
                .verifyComplete();

        verify(persistence, times(1)).insertProductAndPrice("test-name", BigDecimal.ONE);
    }

    @Test
    void createProduct_negative() {

        doReturn(Mono.error(new RuntimeException("Error"))).when(persistence).insertProductAndPrice(any(String.class), any(BigDecimal.class));

        productService.createProduct("test-name", BigDecimal.ONE)
                .as(StepVerifier::create)
                .verifyErrorSatisfies(error -> {
                    assertThat(error, instanceOf(RuntimeException.class));
                    assertThat(error.getMessage(), containsString("Error"));
                });

        verify(persistence, times(1)).insertProductAndPrice("test-name", BigDecimal.ONE);
    }

    @Test
    void readProducts_positive() {

        doReturn(Mono.just(List.of(
                Tuples.of(
                        new Product(2L, "test-name-1"),
                        new ProductPrice(3L, null, BigDecimal.ONE, null)
                ),
                Tuples.of(
                        new Product(1L, "test-name-2"),
                        new ProductPrice(1L, null, BigDecimal.TEN, null)
                )
        ))).when(persistence).selectProducts();

        productService.readProducts()
                .as(StepVerifier::create)
                .assertNext(products -> {
                    assertThat(products.size(), is(2));

                    assertThat(products.getFirst().id(), is(2L));
                    assertThat(products.getFirst().name(), is("test-name-1"));
                    assertThat(products.getFirst().price(), is(BigDecimal.ONE));

                    assertThat(products.get(1).id(), is(1L));
                    assertThat(products.get(1).name(), is("test-name-2"));
                    assertThat(products.get(1).price(), is(BigDecimal.TEN));
                })
                .verifyComplete();

        verify(persistence, times(1)).selectProducts();
    }

    @Test
    void readProducts_positive_NoProducts() {

        doReturn(Mono.just(List.of())).when(persistence).selectProducts();

        productService.readProducts()
                .as(StepVerifier::create)
                .assertNext(products ->
                        assertThat(products.size(), is(0))
                )
                .verifyComplete();

        verify(persistence, times(1)).selectProducts();
    }

    @Test
    void readProducts_negative() {

        doReturn(Mono.error(new RuntimeException("Error"))).when(persistence).selectProducts();

        productService.readProducts()
                .as(StepVerifier::create)
                .verifyErrorSatisfies(error -> {
                    assertThat(error, instanceOf(RuntimeException.class));
                    assertThat(error.getMessage(), containsString("Error"));
                });

        verify(persistence, times(1)).selectProducts();
    }

    @Test
    void updateProduct_positive() {

        doReturn(Mono.empty()).when(persistence).updateProduct(any(Long.class), any(String.class));

        productService.updateProduct(1L, "test-new-name")
                .as(StepVerifier::create)
                .verifyComplete();

        verify(persistence, times(1)).updateProduct(1L, "test-new-name");
    }

    @Test
    void updateProduct_negative() {

        doReturn(Mono.error(new RuntimeException("Error"))).when(persistence).updateProduct(any(Long.class), any(String.class));

        productService.updateProduct(1L, "test-new-name")
                .as(StepVerifier::create)
                .verifyErrorSatisfies(error -> {
                    assertThat(error, instanceOf(RuntimeException.class));
                    assertThat(error.getMessage(), containsString("Error"));
                });

        verify(persistence, times(1)).updateProduct(1L, "test-new-name");
    }

    @Test
    void updateProductPrice_positive() {

        doReturn(Mono.empty()).when(persistence).updateProductPrice(any(Long.class), any(BigDecimal.class));

        productService.updateProductPrice(1L, BigDecimal.TEN)
                .as(StepVerifier::create)
                .verifyComplete();

        verify(persistence, times(1)).updateProductPrice(1L, BigDecimal.TEN);
    }

    @Test
    void updateProductPrice_negative() {

        doReturn(Mono.error(new RuntimeException("Error"))).when(persistence).updateProductPrice(any(Long.class), any(BigDecimal.class));

        productService.updateProductPrice(1L, BigDecimal.TEN)
                .as(StepVerifier::create)
                .verifyErrorSatisfies(error -> {
                    assertThat(error, instanceOf(RuntimeException.class));
                    assertThat(error.getMessage(), containsString("Error"));
                });

        verify(persistence, times(1)).updateProductPrice(1L, BigDecimal.TEN);
    }
}

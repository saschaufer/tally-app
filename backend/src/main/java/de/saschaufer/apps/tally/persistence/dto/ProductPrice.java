package de.saschaufer.apps.tally.persistence.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "product_prices")
public class ProductPrice {

    @Id
    private Long id;
    private Long productId;
    private BigDecimal price;
    private Instant validUntil;
}

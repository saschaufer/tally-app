package de.saschaufer.apps.tally.persistence.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "purchases")
public class Purchase {

    @Id
    private Long id;
    private Long userId;
    private Long productPriceId;
    private LocalDateTime timestamp;
}

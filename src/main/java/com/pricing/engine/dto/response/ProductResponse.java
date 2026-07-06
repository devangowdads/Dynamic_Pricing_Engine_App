package com.pricing.engine.dto.response;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Product representation returned to clients")
public class ProductResponse {

    private Long id;
    private String name;
    private BigDecimal basePrice;
    private Integer inventoryCount;

}
package com.pricing.engine.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Computed price for a product at request time")
public class PriceResponse {

    private Long productId;
    private String productName;
    private BigDecimal basePrice;
    private BigDecimal finalPrice;
    private boolean fromCache;
    private LocalDateTime calculatedAt;

}
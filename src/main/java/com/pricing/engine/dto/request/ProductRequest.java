package com.pricing.engine.dto.request;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Payload to create or update a product")
public class ProductRequest {

    @NotBlank(message = "Product name must not be blank")
    @Schema(example = "Wireless Headphones")
    private String name;

    @NotNull(message = "Base price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Base price must be greater than 0")
    @Schema(example = "99.99")
    private BigDecimal basePrice;

    @NotNull(message = "Inventory count is required")
    @Min(value = 0, message = "Inventory count cannot be negative")
    @Schema(example = "150")
    private Integer inventoryCount;
}
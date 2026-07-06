package com.pricing.engine.dto.request;

import java.math.BigDecimal;

import com.pricing.engine.entity.RuleType;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Payload used by admins to create or update a pricing rule")
public class PricingRuleRequest {

    @NotNull(message = "productId is required")
    @Schema(example = "1")
    private Long productId;

    @NotNull(message = "Rule type is required")
    @Schema(example = "SURGE")
    private RuleType type;

    @NotNull(message = "Rule value is required")
    @PositiveOrZero(message = "Rule value cannot be negative")
    @Schema(example = "1.25", description = "Multiplier or flat amount, interpreted by the matching strategy")
    private BigDecimal value;

    @NotBlank(message = "Condition must not be blank")
    @Schema(example = "demand>80", description = "Condition expression evaluated at price-calculation time")
    private String condition;

    @Builder.Default
    @PositiveOrZero(message = "Priority cannot be negative")
    @Schema(example = "1", description = "Lower value = applied earlier")
    private Integer priority = 0;

    @Builder.Default
    @Schema(example = "true")
    private boolean active = true;
}
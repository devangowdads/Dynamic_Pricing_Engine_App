package com.pricing.engine.dto.response;

import java.math.BigDecimal;

import com.pricing.engine.entity.RuleType;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Pricing rule representation returned to clients")
public class PricingRuleResponse {

    private Long id;
    private Long productId;
    private RuleType type;
    private BigDecimal value;
    private String condition;
    private Integer priority;
    private boolean active;
}
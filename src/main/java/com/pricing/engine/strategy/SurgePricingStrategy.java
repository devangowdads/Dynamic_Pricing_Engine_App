package com.pricing.engine.strategy;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Component;

import com.pricing.engine.entity.PricingRule;
import com.pricing.engine.entity.Product;
import com.pricing.engine.entity.RuleType;

import lombok.extern.slf4j.Slf4j;

/**
 * Applies a demand-driven multiplier to the current price.
 * rule.value is treated as a multiplier, e.g. 1.25 = +25% surge.
 * rule.condition is expected in the form "demand>THRESHOLD"; since this
 * assignment doesn't wire in a live demand signal, the threshold is treated
 * as a fixed activation flag (any active SURGE rule is considered triggered).
 * Swap evaluateCondition() for a real demand-service call in production.
 */
@Slf4j
@Component
public class SurgePricingStrategy implements PricingStrategy {

    @Override
    public BigDecimal apply(BigDecimal currentPrice, Product product, PricingRule rule) {
        if (!evaluateCondition(rule)) {
            log.debug("Surge rule {} not triggered for product {}", rule.getId(), product.getId());
            return currentPrice;
        }
        BigDecimal surged = currentPrice.multiply(rule.getValue()).setScale(2, RoundingMode.HALF_UP);
        log.info("Applied surge rule {} to product {}: {} -> {}", rule.getId(), product.getId(), currentPrice, surged);
        return surged;
    }

    private boolean evaluateCondition(PricingRule rule) {
        return rule.isActive();
    }

    @Override
    public RuleType supports() {
        return RuleType.SURGE;
    }
}
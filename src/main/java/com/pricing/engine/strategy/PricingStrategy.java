package com.pricing.engine.strategy;

import java.math.BigDecimal;

import com.pricing.engine.entity.PricingRule;
import com.pricing.engine.entity.Product;
import com.pricing.engine.entity.RuleType;

/**
 * Strategy Pattern contract: one implementation per RuleType.
 * PricingService loops over active rules (sorted by priority) and asks the
 * matching strategy to transform the running price. This keeps each rule
 * type isolated and lets new rule types be added without touching existing
 * strategies or the service orchestration logic.
 */
public interface PricingStrategy {

    /**
     * @param currentPrice the running price before this rule is applied
     * @param product      the product being priced (gives access to inventory, base price, etc.)
     * @param rule         the specific rule instance being evaluated
     * @return the price after this rule has been applied
     */
    BigDecimal apply(BigDecimal currentPrice, Product product, PricingRule rule);

    /**
     * @return the RuleType this strategy knows how to handle
     */
    RuleType supports();
}
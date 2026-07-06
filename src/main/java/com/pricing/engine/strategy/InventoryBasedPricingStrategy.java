package com.pricing.engine.strategy;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Component;

import com.pricing.engine.entity.PricingRule;
import com.pricing.engine.entity.Product;
import com.pricing.engine.entity.RuleType;

import lombok.extern.slf4j.Slf4j;

/**
 * Applies a multiplier when inventory drops below a configured threshold
 * (low stock -> raise price) or rises above it (overstock -> discount),
 * depending on how the condition is written.
 * rule.condition format: "<THRESHOLD" or ">THRESHOLD", e.g. "<10".
 * rule.value is the multiplier applied when the condition matches.
 */
@Slf4j
@Component
public class InventoryBasedPricingStrategy implements PricingStrategy {

    @Override
    public BigDecimal apply(BigDecimal currentPrice, Product product, PricingRule rule) {
        if (!matchesInventory(rule.getCondition(), product.getInventoryCount())) {
            log.debug("Inventory rule {} not triggered for product {}", rule.getId(), product.getId());
            return currentPrice;
        }
        BigDecimal adjusted = currentPrice.multiply(rule.getValue()).setScale(2, RoundingMode.HALF_UP);
        log.info("Applied inventory rule {} to product {}: {} -> {}", rule.getId(), product.getId(), currentPrice, adjusted);
        return adjusted;
    }

    private boolean matchesInventory(String condition, Integer inventoryCount) {
        if (condition == null || inventoryCount == null || condition.length() < 2) {
            return false;
        }
        try {
            char operator = condition.charAt(0);
            int threshold = Integer.parseInt(condition.substring(1).trim());
            return switch (operator) {
                case '<' -> inventoryCount < threshold;
                case '>' -> inventoryCount > threshold;
                default -> false;
            };
        } catch (NumberFormatException e) {
            log.warn("Could not parse inventory condition '{}', rule skipped", condition);
            return false;
        }
    }

    @Override
    public RuleType supports() {
        return RuleType.INVENTORY;
    }
}
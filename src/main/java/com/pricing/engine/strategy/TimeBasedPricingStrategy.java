package com.pricing.engine.strategy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Component;

import com.pricing.engine.entity.PricingRule;
import com.pricing.engine.entity.Product;
import com.pricing.engine.entity.RuleType;

import lombok.extern.slf4j.Slf4j;

/**
 * Applies a multiplier only during a configured time window.
 * rule.condition format: "HH:mm-HH:mm", e.g. "22:00-06:00" for a late-night window.
 * rule.value is the multiplier applied when the current time falls inside that window.
 */
@Slf4j
@Component
public class TimeBasedPricingStrategy implements PricingStrategy {

    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    @Override
    public BigDecimal apply(BigDecimal currentPrice, Product product, PricingRule rule) {
        if (!isWithinWindow(rule.getCondition())) {
            log.debug("Time-based rule {} outside its active window for product {}", rule.getId(), product.getId());
            return currentPrice;
        }
        BigDecimal adjusted = currentPrice.multiply(rule.getValue()).setScale(2, RoundingMode.HALF_UP);
        log.info("Applied time-based rule {} to product {}: {} -> {}", rule.getId(), product.getId(), currentPrice, adjusted);
        return adjusted;
    }

    private boolean isWithinWindow(String condition) {
        try {
            String[] parts = condition.split("-");
            LocalTime start = LocalTime.parse(parts[0].trim(), FORMAT);
            LocalTime end = LocalTime.parse(parts[1].trim(), FORMAT);
            LocalTime now = LocalTime.now();

            if (start.isBefore(end)) {
                return !now.isBefore(start) && !now.isAfter(end);
            }
            // Window wraps past midnight, e.g. 22:00-06:00.
            return !now.isBefore(start) || !now.isAfter(end);
        } catch (Exception e) {
            log.warn("Could not parse time-based condition '{}', rule skipped", condition);
            return false;
        }
    }

    @Override
    public RuleType supports() {
        return RuleType.TIME_BASED;
    }
}
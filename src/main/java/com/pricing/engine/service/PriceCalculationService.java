
package com.pricing.engine.service;

import com.pricing.engine.dto.response.PriceResponse;
import com.pricing.engine.entity.DynamicPrice;
import com.pricing.engine.entity.PricingRule;
import com.pricing.engine.entity.Product;
import com.pricing.engine.entity.RuleType;
import com.pricing.engine.exception.ResourceNotFoundException;
import com.pricing.engine.repository.DynamicPriceRepository;
import com.pricing.engine.repository.PricingRuleRepository;
import com.pricing.engine.repository.ProductRepository;
import com.pricing.engine.strategy.PricingStrategy;
import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Holds the actual @Cacheable price computation.
 *
 * This is deliberately a separate bean from PricingService. Spring's
 * @Cacheable works via a proxy that intercepts calls made from OUTSIDE a
 * bean; a method calling another @Cacheable method on "this" (self-
 * invocation) bypasses that proxy entirely and the annotation is silently
 * ignored. Keeping this method on its own bean means PricingService always
 * calls it through the real Spring-managed proxy, so caching actually
 * takes effect.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PriceCalculationService {

    private final ProductRepository productRepository;
    private final PricingRuleRepository pricingRuleRepository;
    private final DynamicPriceRepository dynamicPriceRepository;
    private final List<PricingStrategy> strategies;

    private Map<RuleType, PricingStrategy> strategyMap;

    private Map<RuleType, PricingStrategy> strategyMap() {
        if (strategyMap == null) {
            strategyMap = strategies.stream()
                    .collect(Collectors.toMap(PricingStrategy::supports, Function.identity()));
        }
        return strategyMap;
    }

    /**
     * Conflict resolution policy for overlapping rules: rules are applied in
     * ascending priority order and compose multiplicatively (each strategy
     * receives the output of the previous one as its input).
     */
    @Transactional
    public PriceResponse computePrice(Long productId) {
        log.info("Cache miss for product id={}, recalculating price", productId);

        Product product = productRepository.findWithRulesById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        List<PricingRule> activeRules = pricingRuleRepository
                .findByProductIdAndActiveTrueOrderByPriorityAsc(productId);

        BigDecimal finalPrice = evaluateRules(product, activeRules);

        persistAuditRecord(productId, finalPrice);

        return PriceResponse.builder()
                .productId(product.getId())
                .productName(product.getName())
                .basePrice(product.getBasePrice())
                .finalPrice(finalPrice)
                .fromCache(false)
                .calculatedAt(LocalDateTime.now())
                .build();
    }

    private BigDecimal evaluateRules(Product product, List<PricingRule> rules) {
        BigDecimal runningPrice = product.getBasePrice();
        for (PricingRule rule : rules) {
            PricingStrategy strategy = strategyMap().get(rule.getType());
            if (strategy == null) {
                log.warn("No strategy registered for rule type {}, skipping rule id={}",
                        rule.getType(), rule.getId());
                continue;
            }
            runningPrice = strategy.apply(runningPrice, product, rule);
        }
        return runningPrice;
    }

    private void persistAuditRecord(Long productId, BigDecimal finalPrice) {
        try {
            DynamicPrice record = DynamicPrice.builder()
                    .productId(productId)
                    .finalPrice(finalPrice)
                    .timestamp(LocalDateTime.now())
                    .build();
            dynamicPriceRepository.save(record);
        } catch (OptimisticLockException e) {
            log.warn("Skipped audit record for product id={} due to concurrent update", productId);
        }
    }
}
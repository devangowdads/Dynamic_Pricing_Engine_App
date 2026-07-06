package com.pricing.engine.service;

import java.util.List;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pricing.engine.dto.request.PricingRuleRequest;
import com.pricing.engine.dto.response.PricingRuleResponse;
import com.pricing.engine.entity.PricingRule;
import com.pricing.engine.entity.Product;
import com.pricing.engine.exception.ResourceNotFoundException;
import com.pricing.engine.repository.PricingRuleRepository;
import com.pricing.engine.repository.ProductRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PricingRuleService {

    private final PricingRuleRepository pricingRuleRepository;
    private final ProductRepository productRepository;

    @Transactional
    @CacheEvict(value = "priceCache", key = "#request.productId")
    public PricingRuleResponse createRule(PricingRuleRequest request) {
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found with id: " + request.getProductId()));

        PricingRule rule = PricingRule.builder()
                .product(product)
                .type(request.getType())
                .value(request.getValue())
                .condition(request.getCondition())
                .priority(request.getPriority())
                .active(request.isActive())
                .build();

        PricingRule saved = pricingRuleRepository.save(rule);
        log.info("Created {} rule id={} for product id={}, cache evicted",
                saved.getType(), saved.getId(), product.getId());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<PricingRuleResponse> getRulesForProduct(Long productId) {
        return pricingRuleRepository.findByProductIdAndActiveTrueOrderByPriorityAsc(productId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    @CacheEvict(value = "priceCache", key = "#request.productId")
    public PricingRuleResponse updateRule(Long ruleId, PricingRuleRequest request) {
        PricingRule rule = pricingRuleRepository.findById(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("Pricing rule not found with id: " + ruleId));

        rule.setType(request.getType());
        rule.setValue(request.getValue());
        rule.setCondition(request.getCondition());
        rule.setPriority(request.getPriority());
        rule.setActive(request.isActive());

        PricingRule saved = pricingRuleRepository.save(rule);
        log.info("Updated rule id={}, cache evicted for product id={}", ruleId, request.getProductId());
        return toResponse(saved);
    }

    @Transactional
    public void deleteRule(Long ruleId) {
        PricingRule rule = pricingRuleRepository.findById(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("Pricing rule not found with id: " + ruleId));
        Long productId = rule.getProduct().getId();
        pricingRuleRepository.delete(rule);
        evictAfterDelete(productId);
        log.info("Deleted rule id={} for product id={}, cache evicted", ruleId, productId);
    }

    // Separate method so @CacheEvict can bind a key that isn't available as
    // a direct method parameter on deleteRule (productId is derived first).
    @CacheEvict(value = "priceCache", key = "#productId")
    public void evictAfterDelete(Long productId) {
        // no-op body; eviction happens via the annotation
    }

    private PricingRuleResponse toResponse(PricingRule rule) {
        return PricingRuleResponse.builder()
                .id(rule.getId())
                .productId(rule.getProduct().getId())
                .type(rule.getType())
                .value(rule.getValue())
                .condition(rule.getCondition())
                .priority(rule.getPriority())
                .active(rule.isActive())
                .build();
    }
}
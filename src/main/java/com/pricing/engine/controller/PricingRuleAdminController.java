package com.pricing.engine.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pricing.engine.dto.request.PricingRuleRequest;
import com.pricing.engine.dto.response.PricingRuleResponse;
import com.pricing.engine.service.PricingRuleService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/admin/rules")
@RequiredArgsConstructor
@Tag(name = "Admin - Pricing Rules", description = "Admin endpoints to configure pricing rules")
public class PricingRuleAdminController {

    private final PricingRuleService pricingRuleService;

    @PostMapping
    @Operation(summary = "Create a new pricing rule for a product")
    public ResponseEntity<PricingRuleResponse> create(@Valid @RequestBody PricingRuleRequest request) {
        log.info("Admin creating {} rule for product id={}", request.getType(), request.getProductId());
        return ResponseEntity.status(HttpStatus.CREATED).body(pricingRuleService.createRule(request));
    }

    @GetMapping("/product/{productId}")
    @Operation(summary = "List active pricing rules for a product")
    public ResponseEntity<List<PricingRuleResponse>> getForProduct(@PathVariable Long productId) {
        return ResponseEntity.ok(pricingRuleService.getRulesForProduct(productId));
    }

    @PutMapping("/{ruleId}")
    @Operation(summary = "Update an existing pricing rule")
    public ResponseEntity<PricingRuleResponse> update(@PathVariable Long ruleId,
                                                        @Valid @RequestBody PricingRuleRequest request) {
        log.info("Admin updating rule id={}", ruleId);
        return ResponseEntity.ok(pricingRuleService.updateRule(ruleId, request));
    }

    @DeleteMapping("/{ruleId}")
    @Operation(summary = "Delete a pricing rule")
    public ResponseEntity<Void> delete(@PathVariable Long ruleId) {
        log.info("Admin deleting rule id={}", ruleId);
        pricingRuleService.deleteRule(ruleId);
        return ResponseEntity.noContent().build();
    }
}
package com.pricing.engine.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pricing.engine.dto.response.PriceResponse;
import com.pricing.engine.service.PricingService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/price")
@RequiredArgsConstructor
@Tag(name = "Price", description = "Public endpoint for computing dynamic prices")
public class PriceController {

    private final PricingService pricingService;

    @GetMapping("/{productId}")
    @Operation(summary = "Get the current dynamic price for a product")
    public ResponseEntity<PriceResponse> getPrice(@PathVariable Long productId) {
        log.info("Received price request for product id={}", productId);
        PriceResponse response = pricingService.getPrice(productId);
        return ResponseEntity.ok(response);
    }
}


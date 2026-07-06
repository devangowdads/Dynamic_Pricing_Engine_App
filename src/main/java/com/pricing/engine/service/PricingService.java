

package com.pricing.engine.service;

import com.pricing.engine.dto.response.PriceResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

/**
 * Public entry point for price lookups.
 *
 * Caching here is managed explicitly (manual get/put against the
 * CacheManager) rather than via @Cacheable. This makes the hit/miss
 * decision and the fromCache flag fully deterministic and easy to verify,
 * rather than depending on Spring's caching AOP proxy behaving as expected
 * in combination with other annotations on the same method.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PricingService {

    private static final String CACHE_NAME = "priceCache";

    private final PriceCalculationService priceCalculationService;
    private final CacheManager cacheManager;

    public PriceResponse getPrice(Long productId) {
        Cache cache = cacheManager.getCache(CACHE_NAME);

        if (cache != null) {
            Cache.ValueWrapper wrapper = cache.get(productId);
            if (wrapper != null) {
                PriceResponse cached = (PriceResponse) wrapper.get();
                log.info("Cache HIT for product id={}", productId);
                return cached.toBuilder().fromCache(true).build();
            }
        }

        log.info("Cache MISS for product id={}", productId);
        PriceResponse computed = priceCalculationService.computePrice(productId);

        if (cache != null) {
            cache.put(productId, computed);
        }

        return computed.toBuilder().fromCache(false).build();
    }

    /**
     * Called by ProductService / PricingRuleService whenever a product or
     * its rules change, so the next lookup recomputes instead of serving a
     * stale cached price.
     */
    public void evict(Long productId) {
        Cache cache = cacheManager.getCache(CACHE_NAME);
        if (cache != null) {
            cache.evict(productId);
            log.info("Evicted cache entry for product id={}", productId);
        }
    }
}
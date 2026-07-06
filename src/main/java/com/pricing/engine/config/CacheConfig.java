package com.pricing.engine.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Simple in-memory cache manager for the price lookup path.
 * The "priceCache" name is referenced from PricingService via @Cacheable / @CacheEvict.
 * Swap this bean for a Redis-backed CacheManager in production to share cache
 * state across multiple service instances.
 */
@Configuration
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager("priceCache");
    }
}
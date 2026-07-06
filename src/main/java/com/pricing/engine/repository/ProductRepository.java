package com.pricing.engine.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.pricing.engine.entity.Product;

public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * Fetches the product together with its pricing rules in a single query
     * (entity graph) instead of lazily loading rules per-product, which is
     * what causes N+1 queries during price calculation.
     */
    @EntityGraph(attributePaths = "pricingRules")
    Optional<Product> findWithRulesById(Long id);

    boolean existsByNameIgnoreCase(String name);

    
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}
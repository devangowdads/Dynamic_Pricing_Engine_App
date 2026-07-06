package com.pricing.engine.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pricing.engine.entity.PricingRule;

public interface PricingRuleRepository extends JpaRepository<PricingRule, Long> {

    List<PricingRule> findByProductIdAndActiveTrueOrderByPriorityAsc(Long productId);
}
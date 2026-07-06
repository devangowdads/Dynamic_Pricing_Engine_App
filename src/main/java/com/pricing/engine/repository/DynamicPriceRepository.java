package com.pricing.engine.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pricing.engine.entity.DynamicPrice;

public interface DynamicPriceRepository extends JpaRepository<DynamicPrice, Long> {

    List<DynamicPrice> findByProductIdOrderByTimestampDesc(Long productId);
}
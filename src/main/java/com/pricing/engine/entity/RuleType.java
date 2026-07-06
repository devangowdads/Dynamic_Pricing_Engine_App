package com.pricing.engine.entity;

/**
 * Supported pricing rule categories. Each maps to exactly one PricingStrategy
 * implementation (see the strategy package).
 */
public enum RuleType {
    SURGE,
    TIME_BASED,
    INVENTORY
}
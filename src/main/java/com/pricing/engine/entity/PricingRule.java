package com.pricing.engine.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "pricing_rules")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PricingRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RuleType type;

    // Multiplier or flat amount, interpreted by the matching PricingStrategy.
    // Mapped to "rule_value" instead of "value" because VALUE is a reserved
    // SQL keyword in several databases (including H2), which can cause the
    // table's CREATE TABLE statement to fail silently on startup.
    @Column(name = "rule_value", nullable = false, precision = 12, scale = 4)
    private BigDecimal value;

    // Free-form condition, e.g. "demand>80", "22:00-06:00", "inventory<10".
    // Kept as a simple string so rules stay data-driven and don't need a
    // redeploy to add a new threshold. Mapped to "rule_condition" for the
    // same reserved-keyword safety reason as above.
    @Column(name = "rule_condition", nullable = false)
    private String condition;

    // Lower number = applied first. Used to resolve rule ordering when
    // multiple rules of different types apply to the same product.
    @Builder.Default
    @Column(nullable = false)
    private Integer priority = 0;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    @Version
    private Long version;
}
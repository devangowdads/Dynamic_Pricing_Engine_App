-- Sample seed data for the Dynamic Pricing Engine.
-- Column names match the actual JPA mappings (rule_value / rule_condition,
-- not value / condition, since those are reserved words in several SQL
-- dialects — see PricingRule.java).

-- ---------------------------------------------------------------------
-- Products
-- ---------------------------------------------------------------------
INSERT INTO products (name, base_price, inventory_count, version) VALUES
('Wireless Headphones', 99.99, 150, 0),
('Wireless Mouse Pro', 39.99, 8, 0),
('Mechanical Keyboard', 129.99, 45, 0),
('4K Monitor 27-inch', 349.99, 5, 0),
('USB-C Hub', 24.99, 200, 0);

-- ---------------------------------------------------------------------
-- Pricing Rules
-- product_id below assumes auto-increment starts at 1 and rows insert
-- in the order above (1=Headphones, 2=Mouse, 3=Keyboard, 4=Monitor, 5=Hub).
-- Adjust the product_id values if your table already had rows before
-- this script runs.
-- ---------------------------------------------------------------------

-- Surge pricing on Headphones: +25% when triggered
INSERT INTO pricing_rules (product_id, type, rule_value, rule_condition, priority, active, version) VALUES
(1, 'SURGE', 1.25, 'demand>80', 1, true, 0);

-- Time-based night surcharge on Headphones: +10% between 22:00-06:00
INSERT INTO pricing_rules (product_id, type, rule_value, rule_condition, priority, active, version) VALUES
(1, 'TIME_BASED', 1.10, '22:00-06:00', 2, true, 0);

-- Low-stock surcharge on Mouse (inventory_count = 8, below threshold of 10): +15%
INSERT INTO pricing_rules (product_id, type, rule_value, rule_condition, priority, active, version) VALUES
(2, 'INVENTORY', 1.15, '<10', 1, true, 0);

-- Surge pricing on Keyboard: +20% when triggered
INSERT INTO pricing_rules (product_id, type, rule_value, rule_condition, priority, active, version) VALUES
(3, 'SURGE', 1.20, 'demand>75', 1, true, 0);

-- Low-stock surcharge on Monitor (inventory_count = 5, below threshold of 10): +30%
INSERT INTO pricing_rules (product_id, type, rule_value, rule_condition, priority, active, version) VALUES
(4, 'INVENTORY', 1.30, '<10', 1, true, 0);

-- Overstock discount on USB-C Hub (inventory_count = 200, above threshold of 150): -10%
INSERT INTO pricing_rules (product_id, type, rule_value, rule_condition, priority, active, version) VALUES
(5, 'INVENTORY', 0.90, '>150', 1, true, 0);

-- No pricing rules on Keyboard's TIME_BASED slot intentionally, to also
-- demonstrate a product with a single applicable rule.

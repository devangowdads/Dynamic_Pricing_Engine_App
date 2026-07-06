# Dynamic Pricing Engine

A Spring Boot service that computes product prices dynamically using configurable
surge, time-based, and inventory-based pricing rules.

## Tech Stack

- Java 17, Spring Boot 3.2.5, Maven
- Spring Data JPA + MySQL
- Bean Validation (Hibernate Validator)
- Spring Cache infrastructure (managed manually — see [Caching](#caching) below)
- Lombok
- springdoc-openapi (Swagger UI)
- SLF4J logging throughout

## Project Structure

```
src/main/java/com/pricing/engine
├── config/            SwaggerConfig, CacheConfig
├── controller/        REST controllers (Price, Product, Admin rules)
├── dto/request/       Request DTOs (ProductRequest, PricingRuleRequest)
├── dto/response/      Response DTOs (PriceResponse, ProductResponse, PricingRuleResponse, ErrorResponse)
├── entity/            JPA entities (Product, PricingRule, DynamicPrice, RuleType)
├── exception/         ResourceNotFoundException, DuplicateResourceException, GlobalExceptionHandler
├── repository/        Spring Data repositories
├── service/           PricingService, PriceCalculationService, ProductService, PricingRuleService
└── strategy/          PricingStrategy interface + Surge/TimeBased/Inventory implementations

src/main/resources
├── application.yml    MySQL connection, JPA, logging, Swagger config
└── data.sql           Optional seed data (see Seed Data section)
```

See `ARCHITECTURE.md` for the full layered architecture diagram and module
responsibility breakdown, and `ERD.md` for the entity-relationship diagram
(renders natively on GitHub).

## Setup

1. Create a MySQL database (or let the app create it — `createDatabaseIfNotExist=true` is set):
   ```sql
   CREATE DATABASE pricing_db;
   ```
2. Update credentials in `src/main/resources/application.yml` to match your local MySQL setup:
   ```yaml
   spring:
     datasource:
       url: jdbc:mysql://localhost:3306/pricing_db?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC
       username: root
       password: your_password_here
   ```
3. **Do not** keep a separate `application.properties` alongside `application.yml` —
   Spring Boot loads both, and `application.properties` silently takes priority,
   which will override the MySQL config above. Keep only one config file.
4. Build and run:
   ```
   mvn clean install
   mvn spring-boot:run
   ```
5. Swagger UI: `http://localhost:8080/swagger-ui.html`

## Seed Data (optional)

`data.sql` contains sample products and pricing rules. To have it run automatically
on startup, add to `application.yml`:
```yaml
spring:
  jpa:
    defer-datasource-initialization: true   # let Hibernate create tables first
  sql:
    init:
      mode: always                          # run data.sql even with JPA active
```
This file uses plain `INSERT`s assuming an empty schema with auto-increment starting
at 1 — re-running it against a non-empty database will either duplicate rows or trip
the duplicate-product-name check. Drop and recreate the schema between runs if you
want a clean reseed.

## API Overview

| Method | Path | Purpose |
|---|---|---|
| POST | /api/products | Create a product |
| GET | /api/products/{id} | Get a product |
| GET | /api/products | List products |
| PUT | /api/products/{id} | Update a product (evicts price cache) |
| DELETE | /api/products/{id} | Delete a product |
| POST | /api/admin/rules | Create a pricing rule (evicts price cache) |
| GET | /api/admin/rules/product/{productId} | List active rules for a product |
| PUT | /api/admin/rules/{ruleId} | Update a rule (evicts price cache) |
| DELETE | /api/admin/rules/{ruleId} | Delete a rule (evicts price cache) |
| GET | /api/price/{productId} | Get the current computed price |

A Postman collection covering all of the above is available on request.

## How Pricing Works (Strategy Pattern)

`PricingStrategy` is a common interface with one implementation per `RuleType`:

- `SurgePricingStrategy` — multiplies price when a surge rule is active.
- `TimeBasedPricingStrategy` — multiplies price if the current time falls inside the
  rule's `HH:mm-HH:mm` window (the `condition` field), including windows that wrap
  past midnight.
- `InventoryBasedPricingStrategy` — multiplies price when inventory is below/above a
  threshold (condition format `<10` or `>500`).

`PriceCalculationService.computePrice()` loads the product with its active rules in a
single query (`@EntityGraph` on `ProductRepository` — avoids N+1), sorts rules by
`priority` ascending, and threads the price through each matching strategy in turn.

**Conflict resolution policy**: rules are applied in ascending priority order and
compose multiplicatively — each strategy's output becomes the next strategy's input.
This is deterministic and easy to audit. Change `evaluateRules()` in
`PriceCalculationService` if a different policy is needed (e.g. "only the single
largest discount wins").

## Caching

Price lookups are cached by `productId` in an in-memory `ConcurrentMapCacheManager`
(single cache, `"priceCache"`).

**Caching is managed explicitly** in `PricingService.getPrice()` via direct
`cache.get()` / `cache.put()` calls, rather than Spring's `@Cacheable` annotation.
This was a deliberate fix: `@Cacheable` on the same method as `@Transactional` did
not reliably populate the cache in this project's environment, so the logic was
rewritten as plain, traceable code instead of relying on Spring AOP behavior that
proved inconsistent. See `PricingService.java` for the full implementation.

Any mutation that could change a product's price — product update, rule create,
rule update, rule delete — evicts that product's cache entry via `@CacheEvict`, so
the next lookup always recomputes with fresh data.

**Scaling note**: the current cache is in-memory and per-instance. If this service
is ever deployed with multiple replicas, replace `CacheConfig`'s
`ConcurrentMapCacheManager` with a shared store (e.g. Redis) so all instances see
the same cached values and the same evictions.

## Concurrency

- `Product` and `PricingRule` carry a `@Version` column (optimistic locking), so a
  rule update racing with a price calculation results in an `OptimisticLockException`
  rather than a silently lost update. `GlobalExceptionHandler` converts that into a
  `409 Conflict` response.
- Writing the `DynamicPrice` audit record is best-effort: if it loses an
  optimistic-lock race, it's logged and skipped rather than failing the price
  response, since the audit log is secondary to returning a correct price.

## Edge Cases Handled

- **Conflicting rules** — resolved by priority order + multiplicative composition.
- **Demand spikes** — surge rule multiplier; swap
  `SurgePricingStrategy.evaluateCondition()` for a real demand feed when available.
- **Rule updates during processing** — optimistic locking + cache eviction on every
  rule mutation.
- **Duplicate product names** — rejected with `409 Conflict` before insert/update
  (`ProductRepository.existsByNameIgnoreCase`).
- **Reserved SQL keywords** — `value` and `condition` are reserved words in several
  SQL dialects (including MySQL/H2 in certain contexts); mapped to `rule_value` and
  `rule_condition` columns to avoid silent `CREATE TABLE` failures.

## Validation

All request DTOs use Bean Validation annotations (`@NotBlank`, `@NotNull`,
`@DecimalMin`, `@PositiveOrZero`); violations are caught by
`GlobalExceptionHandler` and returned as a structured `400` `ErrorResponse` with
per-field messages.

## Known Limitations / Next Steps

- Cache is in-memory only — not shared across multiple app instances (see Caching).
- `SurgePricingStrategy` has no real demand signal wired in yet.
- No automated test suite included yet — recommended next addition: unit tests per
  strategy, plus a Testcontainers-based integration test for `PriceCalculationService`
  against a real MySQL instance.
- No DB-level `UNIQUE` constraint on `products.name` — uniqueness is enforced only in
  the application layer.

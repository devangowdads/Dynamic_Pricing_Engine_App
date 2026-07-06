# Dynamic Pricing Engine — Architecture & Module Flow

## 1. Overview

The Dynamic Pricing Engine is a Spring Boot service that computes a product's
price at request time by applying a chain of configurable rules (surge,
time-based, inventory-based) on top of a base price. It follows a classic
layered architecture, uses the Strategy Pattern for rule evaluation, and
caches computed prices to avoid recomputation on every request.

**Tech stack:** Java 17, Spring Boot 3.2.5, Maven, MySQL, Spring Data JPA,
Bean Validation, Spring Cache (manual management), Lombok, springdoc-openapi
(Swagger), SLF4J logging.

---

## 2. Layered Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                        Controller Layer                     │
│   PriceController · ProductController · PricingRuleAdmin    │
│         Controller — HTTP in/out, request validation        │
└───────────────────────────┬───────────────────────────────--┘
                             │
┌───────────────────────────▼───────────────────────────────--┐
│                         Service Layer                       │
│  PricingService · PriceCalculationService · ProductService  │
│              · PricingRuleService                           │
│      Orchestration, caching decisions, business rules       │
└───────────────────────────┬───────────────────────────────--┘
                             │
┌───────────────────────────▼───────────────────────────────--┐
│                        Strategy Layer                       │
│  PricingStrategy (interface) · SurgePricingStrategy ·        │
│  TimeBasedPricingStrategy · InventoryBasedPricingStrategy    │
│         One implementation per rule type, pluggable         │
└───────────────────────────┬───────────────────────────────--┘
                             │
┌───────────────────────────▼───────────────────────────────--┐
│                       Repository Layer                      │
│  ProductRepository · PricingRuleRepository ·                │
│  DynamicPriceRepository (Spring Data JPA)                   │
└───────────────────────────┬───────────────────────────────--┘
                             │
┌───────────────────────────▼───────────────────────────────--┐
│                            MySQL                             │
│        products · pricing_rules · dynamic_prices             │
└───────────────────────────────────────────────────────────--┘
```

Cross-cutting concerns sit alongside these layers:
- **DTOs** (`dto/request`, `dto/response`) isolate the API contract from
  JPA entities.
- **Exception handling** (`GlobalExceptionHandler`) converts domain
  exceptions into structured HTTP error responses.
- **Config** (`CacheConfig`, `SwaggerConfig`) wires infrastructure beans.

---

## 3. Module Responsibilities

| Module | Responsibility |
|---|---|
| `controller/PriceController` | Public `GET /api/price/{id}` endpoint |
| `controller/ProductController` | Product CRUD |
| `controller/PricingRuleAdminController` | Admin rule CRUD |
| `service/PricingService` | Cache-first orchestration for price lookups |
| `service/PriceCalculationService` | Actual price computation (DB + strategies), no caching logic |
| `service/ProductService` | Product CRUD, duplicate-name guard, cache eviction on update |
| `service/PricingRuleService` | Rule CRUD, cache eviction on any rule change |
| `strategy/PricingStrategy` | Contract every rule type must implement |
| `strategy/*Strategy` | One class per `RuleType`, isolated logic |
| `repository/*` | Spring Data JPA access, including the N+1-avoiding entity graph |
| `entity/*` | JPA-mapped tables |
| `dto/request/*`, `dto/response/*` | Request/response contracts, decoupled from entities |
| `exception/*` | Custom exceptions + centralized handler |
| `config/CacheConfig` | In-memory `CacheManager` bean |
| `config/SwaggerConfig` | OpenAPI metadata |

---

## 4. Request Flow: `GET /api/price/{id}`

```
Client
  │
  ▼
PriceController.getPrice(productId)
  │
  ▼
PricingService.getPrice(productId)
  │
  ├─► cacheManager.getCache("priceCache").get(productId)
  │
  ├── HIT ──► return cached PriceResponse (fromCache=true)
  │            [no DB access at all]
  │
  └── MISS ─► PriceCalculationService.computePrice(productId)
                │
                ├─► ProductRepository.findWithRulesById(id)
                │     (single query, entity graph — avoids N+1)
                │
                ├─► PricingRuleRepository.findByProductIdAndActiveTrueOrderByPriorityAsc(id)
                │
                ├─► for each rule (priority ascending):
                │       strategyMap.get(rule.getType()).apply(price, product, rule)
                │       (Surge → TimeBased → Inventory, multiplicative composition)
                │
                ├─► DynamicPriceRepository.save(auditRecord)
                │
                └─► return PriceResponse (fromCache=false)
              │
              └─► cache.put(productId, response)   [PricingService stores it]
```

**Conflict resolution policy:** rules apply in ascending `priority` order;
each strategy's output feeds into the next strategy's input
(multiplicative composition). This is deterministic and auditable.

---

## 5. Admin Write Flow (Product / Rule mutation)

```
Client
  │
  ▼
ProductController / PricingRuleAdminController
  │
  ▼
ProductService / PricingRuleService
  │
  ├─► validate (Bean Validation on request DTO)
  ├─► duplicate-name check (ProductService only)
  ├─► repository.save(...) [MySQL, optimistic locking via @Version]
  │
  └─► @CacheEvict("priceCache", key = productId)
        → next GET /api/price/{id} is guaranteed a cache MISS
```

---

## 6. Caching Model

- **Store:** `ConcurrentMapCacheManager`, single named cache `"priceCache"`,
  keyed by `productId`.
- **Population:** managed explicitly in `PricingService.getPrice()` — a
  plain `get`/`put` against the `CacheManager`, not `@Cacheable`. This was
  a deliberate choice after `@Cacheable` proved unreliable when combined
  with `@Transactional` on the same method in this project's Spring Boot
  version; explicit management removes the ambiguity.
- **Invalidation:** `@CacheEvict` on `ProductService.updateProduct/delete`
  and `PricingRuleService.createRule/updateRule/deleteRule`, all keyed by
  `productId`, so any change that could affect price forces a recompute.
- **Scope:** in-memory, per application instance. Fine for a single-node
  deployment; would need a shared store (e.g. Redis) if scaled horizontally.

---

## 7. Concurrency Model

- `Product` and `PricingRule` carry a `@Version` column (optimistic
  locking). A write that loses a race with a concurrent update throws
  `OptimisticLockException`, translated by `GlobalExceptionHandler` into a
  `409 Conflict`.
- Writing the `DynamicPrice` audit row is best-effort: an optimistic-lock
  failure there is logged and swallowed rather than failing the price
  response, since the audit trail is secondary to returning a price.

---

## 8. Edge Cases Handled

| Edge case | Handling |
|---|---|
| Conflicting rules | Priority-ordered, multiplicative composition |
| Demand spikes | `SurgePricingStrategy` multiplier (placeholder for a real demand feed) |
| Rule updated mid-request | Optimistic locking + cache eviction on write |
| N+1 queries | `@EntityGraph` join-fetches product + rules in one query |
| Duplicate product names | `existsByNameIgnoreCase` check before insert/update |
| Reserved SQL keywords | `value`/`condition` columns mapped to `rule_value`/`rule_condition` |

---

## 9. Folder Structure

```
src/main/java/com/pricing/engine
├── config/         CacheConfig, SwaggerConfig
├── controller/      PriceController, ProductController, PricingRuleAdminController
├── dto/
│   ├── request/     ProductRequest, PricingRuleRequest
│   └── response/     ProductResponse, PricingRuleResponse, PriceResponse, ErrorResponse
├── entity/          Product, PricingRule, DynamicPrice, RuleType
├── exception/       ResourceNotFoundException, DuplicateResourceException, GlobalExceptionHandler
├── repository/      ProductRepository, PricingRuleRepository, DynamicPriceRepository
├── service/         PricingService, PriceCalculationService, ProductService, PricingRuleService
└── strategy/        PricingStrategy, SurgePricingStrategy, TimeBasedPricingStrategy, InventoryBasedPricingStrategy
```

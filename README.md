# Dealer Inventory — Modular Monolith

Multi-tenant Dealer & Vehicle Inventory module built with **Spring Boot 3.2**, **Spring Security**, **Spring Data JPA**, **Liquibase**, and **PostgreSQL**.

---

## Feature Overview

### Multi-Tenancy
Every resource (dealer, vehicle) is stamped with a `tenant_id` at creation and all queries are automatically scoped to it. Tenant identity is declared by the caller via the `X-Tenant-Id` HTTP header and enforced by a Spring MVC interceptor that runs **after** authentication, guaranteeing the security context is always populated before the tenant check fires.

| Rule | Result |
|---|---|
| `X-Tenant-Id` header missing or blank | `400 Bad Request` |
| Authenticated user's tenant ≠ header value | `403 Forbidden` |
| Resource exists but belongs to a different tenant | `404 Not Found` |
| `GLOBAL_ADMIN` role | No tenant restriction |

### Dealer Management
- Create, read, list, patch, and delete dealers within a tenant
- Subscription tiers: `BASIC` and `PREMIUM`
- Email is unique **per tenant** (same email allowed in different tenants)
- Pagination and sorting on list endpoint

### Vehicle Management
- Vehicles are always linked to a dealer within the same tenant (cross-tenant dealer assignment blocked at service layer)
- Status lifecycle: `AVAILABLE` → `SOLD`
- Rich filtering on the list endpoint: model (partial, case-insensitive), status, price range, and dealer subscription type
- `subscription=PREMIUM` query returns only vehicles whose dealer has a PREMIUM subscription — still scoped to the caller's tenant
- Pagination and sorting on list endpoint

### Admin
- `GET /admin/dealers/countBySubscription` — returns global dealer counts across **all tenants** grouped by subscription type
- Restricted to `ROLE_GLOBAL_ADMIN`; regular users receive `403`

---

## Architecture

```
src/main/java/com/dealersautocenter/inventory/
│
├── DealerInventoryApplication.java          # Spring Boot entry point
│
├── config/
│   ├── SecurityConfig.java                  # HTTP Basic, stateless, in-memory users, method security
│   └── WebConfig.java                       # Registers TenantInterceptor with Spring MVC
│
├── shared/
│   ├── security/
│   │   ├── TenantContext.java               # ThreadLocal holder for current tenant ID
│   │   ├── TenantInterceptor.java           # MVC interceptor: enforces X-Tenant-Id after auth
│   │   └── CustomUserDetails.java           # UserDetails extension carrying tenantId
│   └── exception/
│       ├── ResourceNotFoundException.java
│       ├── CrossTenantAccessException.java
│       └── GlobalExceptionHandler.java      # @RestControllerAdvice — unified error responses
│
└── module/
    ├── dealer/
    │   ├── domain/
    │   │   ├── Dealer.java                  # JPA entity
    │   │   └── SubscriptionType.java        # Enum: BASIC | PREMIUM
    │   ├── dto/
    │   │   ├── DealerRequest.java           # POST body
    │   │   ├── DealerPatchRequest.java      # PATCH body (all fields optional)
    │   │   └── DealerResponse.java          # API response projection
    │   ├── repository/
    │   │   ├── DealerRepository.java        # JPA repository with tenant-scoped finders
    │   │   └── SubscriptionCountProjection  # Interface projection for admin count query
    │   ├── service/
    │   │   └── DealerService.java           # Business logic, tenant enforcement
    │   └── controller/
    │       └── DealerController.java        # REST endpoints
    │
    ├── vehicle/
    │   ├── domain/
    │   │   ├── Vehicle.java                 # JPA entity (FK → dealers)
    │   │   └── VehicleStatus.java           # Enum: AVAILABLE | SOLD
    │   ├── dto/
    │   │   ├── VehicleRequest.java
    │   │   ├── VehiclePatchRequest.java
    │   │   ├── VehicleResponse.java
    │   │   └── VehicleFilterParams.java     # Carries all optional filter dimensions
    │   ├── repository/
    │   │   ├── VehicleRepository.java       # JpaSpecificationExecutor for dynamic queries
    │   │   └── VehicleSpecification.java    # JPA Criteria-based filter builder
    │   ├── service/
    │   │   └── VehicleService.java
    │   └── controller/
    │       └── VehicleController.java
    │
    └── admin/
        └── controller/
            └── AdminController.java         # /admin/** — GLOBAL_ADMIN only
```

---

## Data Model

```
dealers
  id               UUID  PK
  tenant_id        VARCHAR(100)  NOT NULL
  name             VARCHAR(255)  NOT NULL
  email            VARCHAR(255)  NOT NULL
  subscription_type VARCHAR(20)  NOT NULL  [BASIC | PREMIUM]
  created_at       TIMESTAMPTZ
  updated_at       TIMESTAMPTZ

  UNIQUE (tenant_id, email)
  INDEX  (tenant_id)

vehicles
  id        UUID  PK
  tenant_id VARCHAR(100)  NOT NULL
  dealer_id UUID  NOT NULL  FK → dealers(id)
  model     VARCHAR(255)  NOT NULL
  price     DECIMAL(15,2) NOT NULL
  status    VARCHAR(20)   NOT NULL  [AVAILABLE | SOLD]
  created_at TIMESTAMPTZ
  updated_at TIMESTAMPTZ

  INDEX (tenant_id)
  INDEX (dealer_id)
  INDEX (status)
```

Schema is created automatically on startup by **Liquibase** (`db/changelog/migrations/V001__initial_schema.xml`).

---

## Security Design

```
Request
  │
  ▼
Spring Security Filter Chain
  ├─ BasicAuthenticationFilter     ← resolves username/password, populates SecurityContext
  └─ ExceptionTranslationFilter    ← converts AccessDeniedException → 401/403
  │
  ▼
DispatcherServlet
  │
  ▼
TenantInterceptor.preHandle()      ← MVC interceptor (runs AFTER auth is established)
  ├─ X-Tenant-Id missing?          → 400
  ├─ user.tenantId ≠ header value? → 403
  └─ OK → TenantContext.set(tenantId)
  │
  ▼
Controller → Service → Repository  (all queries scoped with tenantId from TenantContext)
  │
  ▼
TenantInterceptor.afterCompletion() → TenantContext.clear()
```

**Why MVC interceptor, not a servlet filter:**
A `OncePerRequestFilter` registered as a Spring `@Bean` is auto-registered by Spring Boot at the servlet level — before Spring Security runs. This means the `SecurityContext` is empty when the filter executes, so the cross-tenant check silently passes. Using an `HandlerInterceptor` sidesteps this entirely because it runs inside the DispatcherServlet, guaranteed after the full security filter chain completes.

---

## Demo Users

Defined in `SecurityConfig` (replace with a database-backed `UserDetailsService` in production).

| Username | Password | Role | Tenant |
|---|---|---|---|
| `tenant1_user` | `password` | `ROLE_USER` | `tenant-1` |
| `tenant2_user` | `password` | `ROLE_USER` | `tenant-2` |
| `global_admin` | `admin` | `ROLE_GLOBAL_ADMIN` | *(none — unrestricted)* |

---

## Tech Stack

| Layer | Technology |
|---|---|
| Runtime | Java 17, Spring Boot 3.2.5 |
| Web | Spring MVC (embedded Tomcat) |
| Security | Spring Security 6 — HTTP Basic, stateless |
| Persistence | Spring Data JPA, Hibernate 6, PostgreSQL |
| Schema migration | Liquibase 4.24 |
| Validation | Jakarta Bean Validation (Hibernate Validator) |
| Boilerplate reduction | Lombok 1.18.38 |
| Containerisation | Docker Compose, arm64v8/postgres |

---

## Running the Application

### Prerequisites
- Docker Desktop running
- Java 17+, Maven 3.8+

### Start database
```bash
docker-compose up dealer-inventory-db -d
```

### Run application
```bash
# Option A — Maven
mvn spring-boot:run

# Option B — IntelliJ
# Run DealerInventoryApplication (green play button)

# Option C — full Docker stack
docker-compose up --build
```

App starts on `http://localhost:8080`. Liquibase creates the tables on first boot.

### Stop
```bash
# Stop app only
Ctrl+C  (or stop in IntelliJ)

# Stop and remove DB container
docker-compose down

# Stop and wipe all data
docker-compose down -v
```

---

## Acceptance Criteria Verification

| Requirement | Implementation | Status |
|---|---|---|
| Missing `X-Tenant-Id` → `400` | `TenantInterceptor.preHandle()` rejects before hitting any controller | ✅ |
| Cross-tenant access → `403` | Interceptor compares user's stored `tenantId` against header; mismatch returns 403 | ✅ |
| `subscription=PREMIUM` returns only PREMIUM-dealer vehicles within caller's tenant | `VehicleSpecification` joins to dealers table, filters `subscriptionType` AND `tenantId` | ✅ |
| Admin count requires `GLOBAL_ADMIN` | `@PreAuthorize("hasRole('GLOBAL_ADMIN')")` on `AdminController` | ✅ |
| Admin count is global (across all tenants) | JPQL query has no `WHERE tenant_id` clause; documented in `API.md` | ✅ |
| Pagination & sorting on list endpoints | Spring Data `Pageable` with `@PageableDefault` on both list endpoints | ✅ |

---

## Documentation

| File | Contents |
|---|---|
| `README.md` | This file — feature overview, architecture, design decisions |
| `API.md` | Full endpoint reference with request/response examples |
| `DealerInventory.postman_collection.json` | 30 ready-to-run Postman requests covering all scenarios |

# Dealer Inventory API Documentation

## Overview

Multi-tenant REST API for managing **Dealers** and their **Vehicles** within a Modular Monolith application.

Every request is scoped to a tenant via the `X-Tenant-Id` header. Authentication uses HTTP Basic. All timestamps are ISO-8601 with timezone offset.

---

## Base URL

```
http://localhost:8080
```

---

## Authentication

HTTP Basic Authentication is required on every endpoint.

| Username | Password | Role | Tenant Scope |
|---|---|---|---|
| `tenant1_user` | `password` | `ROLE_USER` | `tenant-1` only |
| `tenant2_user` | `password` | `ROLE_USER` | `tenant-2` only |
| `global_admin` | `admin` | `ROLE_GLOBAL_ADMIN` | Unrestricted |

---

## Tenant Enforcement

All non-admin endpoints require the `X-Tenant-Id` header.

| Scenario | HTTP Status |
|---|---|
| `X-Tenant-Id` header missing or blank | `400 Bad Request` |
| Authenticated user's tenant ≠ `X-Tenant-Id` value | `403 Forbidden` |
| Resource belongs to a different tenant | `404 Not Found` |
| `GLOBAL_ADMIN` may pass any `X-Tenant-Id` | `200 OK` |

---

## Common Error Responses

```json
// 400 – Validation / bad input
{ "errors": { "fieldName": "error message" } }

// 400 – Business rule violation
{ "error": "A dealer with email 'x@y.com' already exists in this tenant" }

// 403 – Access denied
{ "error": "Cross-tenant access denied" }

// 404 – Not found
{ "error": "Dealer not found: <uuid>" }
```

---

## Pagination

All list endpoints support Spring Data pagination query parameters:

| Parameter | Default | Description |
|---|---|---|
| `page` | `0` | Zero-based page number |
| `size` | `20` | Page size |
| `sort` | varies | Field and direction, e.g. `name,asc` or `price,desc` |

**Response envelope:**
```json
{
  "content": [ ...items... ],
  "pageable": { "pageNumber": 0, "pageSize": 20 },
  "totalElements": 42,
  "totalPages": 3,
  "last": false
}
```

---

## Data Model

### Dealer

| Field | Type | Constraints |
|---|---|---|
| `id` | UUID | Auto-generated, immutable |
| `tenantId` | string | Set from `X-Tenant-Id`, immutable |
| `name` | string | Required, non-blank |
| `email` | string | Required, valid email, unique per tenant |
| `subscriptionType` | enum | `BASIC` \| `PREMIUM` |
| `createdAt` | OffsetDateTime | Auto-set on create |
| `updatedAt` | OffsetDateTime | Auto-updated on save |

### Vehicle

| Field | Type | Constraints |
|---|---|---|
| `id` | UUID | Auto-generated, immutable |
| `tenantId` | string | Set from `X-Tenant-Id`, immutable |
| `dealerId` | UUID | FK to dealers, must belong to same tenant |
| `dealerName` | string | Denormalized from dealer |
| `dealerSubscriptionType` | enum | Denormalized from dealer |
| `model` | string | Required, non-blank |
| `price` | decimal | Required, positive |
| `status` | enum | `AVAILABLE` \| `SOLD` |
| `createdAt` | OffsetDateTime | Auto-set on create |
| `updatedAt` | OffsetDateTime | Auto-updated on save |

---

## Dealer Endpoints

### POST /dealers
Create a new dealer for the caller's tenant.

**Headers**
```
Authorization: Basic <base64>
X-Tenant-Id: tenant-1
Content-Type: application/json
```

**Request Body**
```json
{
  "name": "ABC Motors",
  "email": "abc@motors.com",
  "subscriptionType": "PREMIUM"
}
```

| Field | Required | Validation |
|---|---|---|
| `name` | Yes | Non-blank |
| `email` | Yes | Valid email format, unique within tenant |
| `subscriptionType` | Yes | `BASIC` or `PREMIUM` |

**Response `201 Created`**
```json
{
  "id": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "tenantId": "tenant-1",
  "name": "ABC Motors",
  "email": "abc@motors.com",
  "subscriptionType": "PREMIUM",
  "createdAt": "2026-04-27T10:00:00+05:30",
  "updatedAt": "2026-04-27T10:00:00+05:30"
}
```

**Error Responses**
| Status | Condition |
|---|---|
| `400` | Missing/blank fields, invalid email, duplicate email in tenant |
| `400` | Missing `X-Tenant-Id` header |
| `403` | User's tenant ≠ `X-Tenant-Id` |
| `401` | No / invalid credentials |

---

### GET /dealers/{id}
Retrieve a single dealer by ID, scoped to the caller's tenant.

**Headers**
```
Authorization: Basic <base64>
X-Tenant-Id: tenant-1
```

**Path Parameters**
| Parameter | Type | Description |
|---|---|---|
| `id` | UUID | Dealer identifier |

**Response `200 OK`**
```json
{
  "id": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "tenantId": "tenant-1",
  "name": "ABC Motors",
  "email": "abc@motors.com",
  "subscriptionType": "PREMIUM",
  "createdAt": "2026-04-27T10:00:00+05:30",
  "updatedAt": "2026-04-27T10:00:00+05:30"
}
```

**Error Responses**
| Status | Condition |
|---|---|
| `404` | Dealer not found or belongs to a different tenant |

---

### GET /dealers
List all dealers for the caller's tenant with pagination.

**Headers**
```
Authorization: Basic <base64>
X-Tenant-Id: tenant-1
```

**Query Parameters**
| Parameter | Type | Default | Description |
|---|---|---|---|
| `page` | int | `0` | Page number |
| `size` | int | `20` | Page size |
| `sort` | string | `name,asc` | Sort field and direction |

**Example**
```
GET /dealers?page=0&size=10&sort=email,desc
```

**Response `200 OK`** — Page of `DealerResponse`

---

### PATCH /dealers/{id}
Partially update a dealer. Only supplied (non-null) fields are applied.

**Headers**
```
Authorization: Basic <base64>
X-Tenant-Id: tenant-1
Content-Type: application/json
```

**Request Body** _(all fields optional)_
```json
{
  "name": "ABC Motors Renamed",
  "email": "new@email.com",
  "subscriptionType": "BASIC"
}
```

**Response `200 OK`** — Updated `DealerResponse`

**Error Responses**
| Status | Condition |
|---|---|
| `400` | New email already used by another dealer in the tenant |
| `404` | Dealer not found |

---

### DELETE /dealers/{id}
Delete a dealer. The dealer must have no vehicles or those vehicles must be deleted first (FK constraint).

**Headers**
```
Authorization: Basic <base64>
X-Tenant-Id: tenant-1
```

**Response `204 No Content`**

**Error Responses**
| Status | Condition |
|---|---|
| `404` | Dealer not found |

---

## Vehicle Endpoints

### POST /vehicles
Create a new vehicle linked to a dealer in the caller's tenant.

**Headers**
```
Authorization: Basic <base64>
X-Tenant-Id: tenant-1
Content-Type: application/json
```

**Request Body**
```json
{
  "dealerId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "model": "Toyota Camry 2024",
  "price": 28500.00,
  "status": "AVAILABLE"
}
```

| Field | Required | Validation |
|---|---|---|
| `dealerId` | Yes | Must reference a dealer in the same tenant |
| `model` | Yes | Non-blank |
| `price` | Yes | Positive decimal |
| `status` | Yes | `AVAILABLE` or `SOLD` |

**Response `201 Created`**
```json
{
  "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "tenantId": "tenant-1",
  "dealerId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "dealerName": "ABC Motors",
  "dealerSubscriptionType": "PREMIUM",
  "model": "Toyota Camry 2024",
  "price": 28500.00,
  "status": "AVAILABLE",
  "createdAt": "2026-04-27T11:00:00+05:30",
  "updatedAt": "2026-04-27T11:00:00+05:30"
}
```

**Error Responses**
| Status | Condition |
|---|---|
| `400` | Missing/invalid fields |
| `404` | `dealerId` not found in the caller's tenant |

---

### GET /vehicles/{id}
Retrieve a single vehicle by ID, scoped to the caller's tenant.

**Response `200 OK`** — `VehicleResponse`

**Error Responses**
| Status | Condition |
|---|---|
| `404` | Vehicle not found or belongs to a different tenant |

---

### GET /vehicles
List vehicles with optional filters and pagination.

**Headers**
```
Authorization: Basic <base64>
X-Tenant-Id: tenant-1
```

**Query Parameters**
| Parameter | Type | Description |
|---|---|---|
| `model` | string | Case-insensitive partial match on model name |
| `status` | enum | `AVAILABLE` or `SOLD` |
| `priceMin` | decimal | Minimum price (inclusive) |
| `priceMax` | decimal | Maximum price (inclusive) |
| `subscription` | enum | `BASIC` or `PREMIUM` — filters by dealer's subscription type |
| `page` | int | Page number (default `0`) |
| `size` | int | Page size (default `20`) |
| `sort` | string | Sort field and direction (default `model,asc`) |

All filters are optional and combinable.

**Examples**
```
GET /vehicles
GET /vehicles?status=AVAILABLE
GET /vehicles?model=toyota&status=AVAILABLE
GET /vehicles?priceMin=20000&priceMax=50000
GET /vehicles?subscription=PREMIUM
GET /vehicles?model=bmw&priceMin=60000&subscription=PREMIUM&sort=price,desc
```

#### subscription=PREMIUM behaviour

Returns vehicles **whose dealer has `subscriptionType=PREMIUM`**, while still enforcing tenant scope.

```
caller tenant: tenant-1
X-Tenant-Id:   tenant-1
subscription:  PREMIUM

→ Returns vehicles belonging to PREMIUM dealers within tenant-1 only.
   Vehicles from other tenants are never included regardless of dealer type.
```

**Response `200 OK`** — Page of `VehicleResponse`

---

### PATCH /vehicles/{id}
Partially update a vehicle. Only supplied (non-null) fields are applied.

**Request Body** _(all fields optional)_
```json
{
  "dealerId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "model": "Toyota Camry 2025",
  "price": 30000.00,
  "status": "SOLD"
}
```

**Response `200 OK`** — Updated `VehicleResponse`

**Error Responses**
| Status | Condition |
|---|---|
| `400` | Price is not positive |
| `404` | Vehicle not found, or new `dealerId` not found in tenant |

---

### DELETE /vehicles/{id}
Delete a vehicle.

**Response `204 No Content`**

**Error Responses**
| Status | Condition |
|---|---|
| `404` | Vehicle not found |

---

## Admin Endpoints

> Requires `ROLE_GLOBAL_ADMIN`. Regular users receive `403 Forbidden`.

### GET /admin/dealers/countBySubscription
Returns the total count of dealers grouped by subscription type **across all tenants** (global, not per-tenant).

**Headers**
```
Authorization: Basic Z2xvYmFsX2FkbWluOmFkbWlu
X-Tenant-Id: <any value>
```

> `X-Tenant-Id` is still required by the filter but GLOBAL_ADMIN is not restricted to it.
> The count itself aggregates all tenants — it is intentionally global.

**Response `200 OK`**
```json
{
  "BASIC": 5,
  "PREMIUM": 3
}
```

Both keys are always present. If no dealers exist for a type its value is `0`.

**Error Responses**
| Status | Condition |
|---|---|
| `403` | Caller does not have `ROLE_GLOBAL_ADMIN` |
| `401` | No / invalid credentials |

---

## Enum Reference

### SubscriptionType
| Value | Description |
|---|---|
| `BASIC` | Basic subscription tier |
| `PREMIUM` | Premium subscription tier |

### VehicleStatus
| Value | Description |
|---|---|
| `AVAILABLE` | Vehicle is available for sale |
| `SOLD` | Vehicle has been sold |

---

## HTTP Status Code Summary

| Code | Meaning |
|---|---|
| `200` | OK — request succeeded |
| `201` | Created — resource created successfully |
| `204` | No Content — deletion succeeded |
| `400` | Bad Request — validation error, missing header, or business rule violation |
| `401` | Unauthorized — missing or invalid credentials |
| `403` | Forbidden — cross-tenant access or insufficient role |
| `404` | Not Found — resource does not exist in this tenant |

---

## Running Locally

### Start the database
```bash
docker-compose up dealer-inventory-db -d
```

### Run the application
```bash
# Maven
mvn spring-boot:run

# Or in IntelliJ — run DealerInventoryApplication
```

### Quick smoke test
```bash
# Create a dealer
curl -u tenant1_user:password \
  -H "X-Tenant-Id: tenant-1" \
  -H "Content-Type: application/json" \
  -X POST http://localhost:8080/dealers \
  -d '{"name":"ABC Motors","email":"abc@motors.com","subscriptionType":"PREMIUM"}'

# List vehicles with PREMIUM filter
curl -u tenant1_user:password \
  -H "X-Tenant-Id: tenant-1" \
  "http://localhost:8080/vehicles?subscription=PREMIUM"

# Admin count
curl -u global_admin:admin \
  -H "X-Tenant-Id: tenant-1" \
  http://localhost:8080/admin/dealers/countBySubscription
```

### Postman
Import `DealerInventory.postman_collection.json` — all 30 requests pre-configured with auto-captured IDs.

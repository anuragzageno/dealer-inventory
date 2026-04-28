# Request Flow — End to End

Tracing a real request through every layer:

```
GET /dealers
Authorization: Basic dGVuYW50MV91c2VyOnBhc3N3b3Jk
X-Tenant-Id: tenant-1
```

---

## Layer 1 — Tomcat (HTTP Server)

The raw HTTP packet arrives. Tomcat parses it into an `HttpServletRequest` object and hands it to the **Spring Security Filter Chain**.

---

## Layer 2 — Spring Security Filter Chain

These run in order on every request:

**`BasicAuthenticationFilter`** ← the important one
1. Sees `Authorization: Basic dGVuYW50MV91c2VyOnBhc3N3b3Jk`
2. Base64 decodes it → `tenant1_user:password`
3. Calls `DatabaseUserDetailsService.loadUserByUsername("tenant1_user")`
4. That hits the DB: `SELECT * FROM app_users WHERE username = 'tenant1_user'`
5. Gets back `AppUser` → wraps it in `CustomUserDetails(username, "{noop}password", "tenant-1", "ROLE_USER")`
6. Calls `passwordEncoder.matches("password", "{noop}password")` → reads `{noop}` prefix → plain compare → ✅
7. Creates an `Authentication` object wrapping the `CustomUserDetails`
8. Stores it: `SecurityContextHolder.getContext().setAuthentication(auth)`

**`AuthorizationFilter`** (last in chain)
- Checks: is this `/admin/**`? No → just needs to be authenticated → ✅ passes

---

## Layer 3 — TenantInterceptor (MVC Layer)

Runs `preHandle()` **after** the entire security chain:

```java
String tenantId = request.getHeader("X-Tenant-Id"); // → "tenant-1"

// Not blank → passes first check

Authentication auth = SecurityContextHolder.getContext().getAuthentication();
CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
String userTenantId = userDetails.getTenantId(); // → "tenant-1" (from DB)

userTenantId.equals(tenantId) // "tenant-1".equals("tenant-1") → true → passes

TenantContext.set("tenant-1"); // stores in ThreadLocal for this request thread
```

---

## Layer 4 — Controller

```java
@GetMapping
public Page<DealerResponse> getAll(@PageableDefault Pageable pageable) {
    return dealerService.findAll(pageable);
}
```

Just receives the request and delegates to the service. No security logic here.

---

## Layer 5 — Service

```java
public Page<DealerResponse> findAll(Pageable pageable) {
    return dealerRepository.findAllByTenantId(TenantContext.get(), pageable)
            .map(DealerResponse::from);
}
```

`TenantContext.get()` returns `"tenant-1"` (set in Layer 3). This is the tenant isolation — **every single query is automatically scoped**.

---

## Layer 6 — Repository → Database

Spring Data JPA translates:
```java
findAllByTenantId("tenant-1", pageable)
```
into:
```sql
SELECT * FROM dealers WHERE tenant_id = 'tenant-1'
ORDER BY name ASC LIMIT 20 OFFSET 0
```

Only tenant-1's dealers come back. Even if tenant-2's dealers exist in the same table, they are **never fetched**.

---

## Layer 7 — Response

Results mapped to `DealerResponse` DTOs → serialized to JSON → sent back as `200 OK`.

After the response is sent, `TenantInterceptor.afterCompletion()` calls:
```java
TenantContext.clear(); // remove from ThreadLocal so next request on this thread starts clean
```

---

## Full Picture

```
Request
  │
  ├─ Tomcat          → parses HTTP
  ├─ BasicAuthFilter → authenticates user, puts CustomUserDetails in SecurityContext
  ├─ AuthFilter      → checks role (GLOBAL_ADMIN for /admin/**, else just authenticated)
  ├─ TenantIntercept → validates X-Tenant-Id, sets TenantContext
  ├─ Controller      → routes to service
  ├─ Service         → TenantContext.get() scopes all queries
  ├─ Repository      → SQL with WHERE tenant_id = ?
  └─ Response        → JSON + TenantContext.clear()
```

---

## What Happens When Something Fails

| Layer | What fails | HTTP Response |
|---|---|---|
| Layer 2 | Wrong password | `401 Unauthorized` |
| Layer 2 | User doesn't exist | `401 Unauthorized` |
| Layer 2 | Not GLOBAL_ADMIN on `/admin/**` | `403 Forbidden` |
| Layer 3 | Missing `X-Tenant-Id` header | `400 Bad Request` |
| Layer 3 | `tenant1_user` sends `X-Tenant-Id: tenant-2` | `403 Forbidden` |
| Layer 5/6 | Resource not found in tenant | `404 Not Found` |

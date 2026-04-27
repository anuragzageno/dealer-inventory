package com.dealersautocenter.inventory.shared.security;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * MVC interceptor that enforces multi-tenancy via the {@code X-Tenant-Id} HTTP header.
 *
 * <p>Runs AFTER Spring Security's filter chain, so the {@link SecurityContextHolder}
 * is always populated with the authenticated user when this interceptor executes.
 * This eliminates the filter-ordering issues that exist with a {@code OncePerRequestFilter}
 * approach (where Boot auto-registration can cause the check to run before authentication).
 *
 * <p>Rules:
 * <ol>
 *   <li>All requests must supply {@code X-Tenant-Id}; missing/blank → 400 Bad Request.</li>
 *   <li>Authenticated regular users: their stored {@code tenantId} must equal the header value;
 *       mismatch → 403 Forbidden.</li>
 *   <li>GLOBAL_ADMIN users (tenantId == null) bypass the tenant-matching check.</li>
 * </ol>
 */
@Component
public class TenantInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        // Internal error dispatches re-use the same request; let them through.
        if (request.getDispatcherType() == DispatcherType.ERROR) {
            return true;
        }

        String tenantId = request.getHeader("X-Tenant-Id");

        if (tenantId == null || tenantId.isBlank()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                    "Missing or empty X-Tenant-Id header");
            return false;
        }

        // Cross-tenant check — SecurityContext is fully populated at this point.
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()
                && auth.getPrincipal() instanceof CustomUserDetails userDetails) {

            String userTenantId = userDetails.getTenantId();
            // null tenantId  →  GLOBAL_ADMIN, no restriction
            if (userTenantId != null && !userTenantId.equals(tenantId)) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN,
                        "Cross-tenant access denied");
                return false;
            }
        }

        TenantContext.set(tenantId);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler,
                                Exception ex) {
        TenantContext.clear();
    }
}

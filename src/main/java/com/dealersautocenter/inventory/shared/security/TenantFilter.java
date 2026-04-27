package com.dealersautocenter.inventory.shared.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Servlet filter that enforces multi-tenancy via the {@code X-Tenant-Id} HTTP header.
 *
 * <p>Rules:
 * <ol>
 *   <li>All requests must supply {@code X-Tenant-Id}; missing/blank → 400 Bad Request.</li>
 *   <li>For authenticated regular users their stored {@code tenantId} must equal the
 *       header value; mismatch → 403 Forbidden.</li>
 *   <li>GLOBAL_ADMIN users (tenantId == null) bypass the tenant-matching check and may
 *       supply any value (useful for cross-tenant operations).</li>
 * </ol>
 *
 * <p>This filter is registered inside the Spring Security filter chain (after
 * {@code BasicAuthenticationFilter}) so authentication is already resolved when it runs.
 */
public class TenantFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        String tenantId = request.getHeader("X-Tenant-Id");

        if (tenantId == null || tenantId.isBlank()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                    "Missing or empty X-Tenant-Id header");
            return;
        }

        // Cross-tenant check: only applicable for authenticated, non-admin users
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()
                && auth.getPrincipal() instanceof CustomUserDetails userDetails) {

            String userTenantId = userDetails.getTenantId();
            // userTenantId == null  →  GLOBAL_ADMIN, skip restriction
            if (userTenantId != null && !userTenantId.equals(tenantId)) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN,
                        "Cross-tenant access denied");
                return;
            }
        }

        TenantContext.set(tenantId);
        try {
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}

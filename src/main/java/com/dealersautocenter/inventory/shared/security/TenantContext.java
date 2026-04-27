package com.dealersautocenter.inventory.shared.security;

/**
 * ThreadLocal holder for the current request's tenant identifier.
 * Set by TenantFilter on every inbound request; cleared after the request completes.
 */
public final class TenantContext {

    private static final ThreadLocal<String> TENANT = new ThreadLocal<>();

    private TenantContext() {}

    public static String get() {
        return TENANT.get();
    }

    public static void set(String tenantId) {
        TENANT.set(tenantId);
    }

    public static void clear() {
        TENANT.remove();
    }
}

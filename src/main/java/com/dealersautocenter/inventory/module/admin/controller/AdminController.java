package com.dealersautocenter.inventory.module.admin.controller;

import com.dealersautocenter.inventory.module.dealer.domain.SubscriptionType;
import com.dealersautocenter.inventory.module.dealer.repository.DealerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Admin endpoints — accessible only to users with the {@code GLOBAL_ADMIN} role.
 */
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('GLOBAL_ADMIN')")
public class AdminController {

    private final DealerRepository dealerRepository;

    /**
     * Returns the total number of dealers grouped by subscription type across <em>all</em> tenants
     * (global count, not per-tenant).
     *
     * <p>Because this endpoint is restricted to GLOBAL_ADMIN, it intentionally aggregates data
     * system-wide and is not filtered by {@code X-Tenant-Id}.
     *
     * <p>Example response:
     * <pre>{@code { "BASIC": 14, "PREMIUM": 7 }}</pre>
     *
     * <p>If no dealers exist for a given subscription type, its count will be {@code 0}.
     */
    @GetMapping("/dealers/countBySubscription")
    public Map<String, Long> countBySubscription() {
        // Pre-populate all types with 0 so the response always contains every key
        Map<String, Long> result = Arrays.stream(SubscriptionType.values())
                .collect(Collectors.toMap(Enum::name, s -> 0L));

        dealerRepository.countGroupedBySubscription()
                .forEach(p -> result.put(p.getSubscriptionType().name(), p.getCount()));

        return result;
    }
}

package com.dealersautocenter.inventory.module.vehicle.repository;

import com.dealersautocenter.inventory.module.dealer.domain.Dealer;
import com.dealersautocenter.inventory.module.vehicle.domain.Vehicle;
import com.dealersautocenter.inventory.module.vehicle.dto.VehicleFilterParams;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * Factory for {@link Specification} objects used when querying vehicles with optional filters.
 *
 * <p>Tenant scoping is always enforced — callers must supply a non-null {@code tenantId}.
 */
public final class VehicleSpecification {

    private VehicleSpecification() {}

    /**
     * Builds a compound specification that:
     * <ol>
     *   <li>Scopes results to {@code tenantId}.</li>
     *   <li>Optionally filters by model (case-insensitive contains).</li>
     *   <li>Optionally filters by status.</li>
     *   <li>Optionally filters by price range.</li>
     *   <li>Optionally filters by the dealer's subscription type (tenant-scoped join).</li>
     * </ol>
     */
    public static Specification<Vehicle> withFilters(String tenantId, VehicleFilterParams params) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Always restrict to the caller's tenant
            predicates.add(cb.equal(root.get("tenantId"), tenantId));

            if (params.model() != null && !params.model().isBlank()) {
                predicates.add(cb.like(
                        cb.lower(root.get("model")),
                        "%" + params.model().toLowerCase() + "%"));
            }

            if (params.status() != null) {
                predicates.add(cb.equal(root.get("status"), params.status()));
            }

            if (params.priceMin() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), params.priceMin()));
            }

            if (params.priceMax() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), params.priceMax()));
            }

            if (params.subscription() != null) {
                // Join to dealers and filter by subscription type.
                // The dealer tenantId check is redundant given the FK + vehicle tenantId check,
                // but adds defence-in-depth against data inconsistencies.
                Join<Vehicle, Dealer> dealerJoin = root.join("dealer", JoinType.INNER);
                predicates.add(cb.equal(dealerJoin.get("subscriptionType"), params.subscription()));
                predicates.add(cb.equal(dealerJoin.get("tenantId"), tenantId));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}

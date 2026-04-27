package com.dealersautocenter.inventory.module.dealer.repository;

import com.dealersautocenter.inventory.module.dealer.domain.Dealer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DealerRepository extends JpaRepository<Dealer, UUID> {

    Optional<Dealer> findByIdAndTenantId(UUID id, String tenantId);

    Page<Dealer> findAllByTenantId(String tenantId, Pageable pageable);

    boolean existsByEmailAndTenantId(String email, String tenantId);

    boolean existsByEmailAndTenantIdAndIdNot(String email, String tenantId, UUID excludeId);

    /**
     * Global count grouped by subscription type — used by GLOBAL_ADMIN only.
     * Counts dealers across ALL tenants.
     */
    @Query("SELECT d.subscriptionType AS subscriptionType, COUNT(d) AS count " +
           "FROM Dealer d GROUP BY d.subscriptionType")
    List<SubscriptionCountProjection> countGroupedBySubscription();
}

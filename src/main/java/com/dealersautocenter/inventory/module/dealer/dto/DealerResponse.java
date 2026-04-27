package com.dealersautocenter.inventory.module.dealer.dto;

import com.dealersautocenter.inventory.module.dealer.domain.Dealer;
import com.dealersautocenter.inventory.module.dealer.domain.SubscriptionType;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DealerResponse(
        UUID id,
        String tenantId,
        String name,
        String email,
        SubscriptionType subscriptionType,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static DealerResponse from(Dealer dealer) {
        return new DealerResponse(
                dealer.getId(),
                dealer.getTenantId(),
                dealer.getName(),
                dealer.getEmail(),
                dealer.getSubscriptionType(),
                dealer.getCreatedAt(),
                dealer.getUpdatedAt()
        );
    }
}

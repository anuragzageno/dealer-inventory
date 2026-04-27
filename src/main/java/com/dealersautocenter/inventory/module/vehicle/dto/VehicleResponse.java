package com.dealersautocenter.inventory.module.vehicle.dto;

import com.dealersautocenter.inventory.module.dealer.domain.SubscriptionType;
import com.dealersautocenter.inventory.module.vehicle.domain.Vehicle;
import com.dealersautocenter.inventory.module.vehicle.domain.VehicleStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record VehicleResponse(
        UUID id,
        String tenantId,
        UUID dealerId,
        String dealerName,
        SubscriptionType dealerSubscriptionType,
        String model,
        BigDecimal price,
        VehicleStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static VehicleResponse from(Vehicle vehicle) {
        return new VehicleResponse(
                vehicle.getId(),
                vehicle.getTenantId(),
                vehicle.getDealer().getId(),
                vehicle.getDealer().getName(),
                vehicle.getDealer().getSubscriptionType(),
                vehicle.getModel(),
                vehicle.getPrice(),
                vehicle.getStatus(),
                vehicle.getCreatedAt(),
                vehicle.getUpdatedAt()
        );
    }
}

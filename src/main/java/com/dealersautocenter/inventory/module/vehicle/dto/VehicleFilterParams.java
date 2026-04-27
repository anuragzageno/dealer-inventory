package com.dealersautocenter.inventory.module.vehicle.dto;

import com.dealersautocenter.inventory.module.dealer.domain.SubscriptionType;
import com.dealersautocenter.inventory.module.vehicle.domain.VehicleStatus;

import java.math.BigDecimal;

/**
 * Holds all optional filter parameters for the vehicle list query.
 * Each field is nullable — a null value means "no filter on this dimension".
 */
public record VehicleFilterParams(
        String model,
        VehicleStatus status,
        BigDecimal priceMin,
        BigDecimal priceMax,
        /** When set, only vehicles whose dealer has this subscription type are returned. */
        SubscriptionType subscription
) {}

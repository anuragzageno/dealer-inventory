package com.dealersautocenter.inventory.module.vehicle.dto;

import com.dealersautocenter.inventory.module.vehicle.domain.VehicleStatus;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * All fields are optional — only non-null fields are applied during a PATCH.
 */
public record VehiclePatchRequest(
        UUID dealerId,
        String model,

        @Positive(message = "Price must be positive")
        BigDecimal price,

        VehicleStatus status
) {}

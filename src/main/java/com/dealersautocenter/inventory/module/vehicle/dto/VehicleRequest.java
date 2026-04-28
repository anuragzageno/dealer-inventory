package com.dealersautocenter.inventory.module.vehicle.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record VehicleRequest(
        @NotNull(message = "Dealer ID is required")
        UUID dealerId,

        @NotBlank(message = "Model is required")
        String model,

        @NotNull(message = "Price is required")
        @Positive(message = "Price must be positive")
        BigDecimal price
) {}

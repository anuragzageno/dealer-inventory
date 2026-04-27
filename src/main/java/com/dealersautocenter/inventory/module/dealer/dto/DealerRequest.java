package com.dealersautocenter.inventory.module.dealer.dto;

import com.dealersautocenter.inventory.module.dealer.domain.SubscriptionType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DealerRequest(
        @NotBlank(message = "Name is required")
        String name,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        String email,

        @NotNull(message = "Subscription type is required")
        SubscriptionType subscriptionType
) {}

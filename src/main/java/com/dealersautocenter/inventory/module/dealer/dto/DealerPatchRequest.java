package com.dealersautocenter.inventory.module.dealer.dto;

import com.dealersautocenter.inventory.module.dealer.domain.SubscriptionType;
import jakarta.validation.constraints.Email;

/**
 * All fields are optional — only non-null fields are applied during a PATCH.
 */
public record DealerPatchRequest(
        String name,

        @Email(message = "Email must be valid")
        String email,

        SubscriptionType subscriptionType
) {}

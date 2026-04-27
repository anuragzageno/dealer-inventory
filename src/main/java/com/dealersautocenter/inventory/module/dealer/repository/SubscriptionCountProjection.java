package com.dealersautocenter.inventory.module.dealer.repository;

import com.dealersautocenter.inventory.module.dealer.domain.SubscriptionType;

/**
 * Projection used by the admin count-by-subscription query.
 */
public interface SubscriptionCountProjection {
    SubscriptionType getSubscriptionType();
    Long getCount();
}

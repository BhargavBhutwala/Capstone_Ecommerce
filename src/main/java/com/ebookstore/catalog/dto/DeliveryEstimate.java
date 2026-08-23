package com.ebookstore.catalog.dto;

/**
 * Delivery estimate embedded in {@link ProductResponse}.
 * Matches the OpenAPI {@code DeliveryEstimate} schema.
 */
public class DeliveryEstimate {

    private final Integer minDays;
    private final Integer maxDays;

    public DeliveryEstimate(Integer minDays, Integer maxDays) {
        this.minDays = minDays;
        this.maxDays = maxDays;
    }

    public Integer getMinDays() { return minDays; }
    public Integer getMaxDays() { return maxDays; }
}

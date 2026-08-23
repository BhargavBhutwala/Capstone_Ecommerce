package com.ebookstore.order.dto;

/**
 * Shipping address snapshot embedded in {@link OrderResponse}.
 *
 * <p>Matches the OpenAPI {@code ShippingAddressSnapshot} schema.
 * These seven fields are copied from the customer's chosen address at checkout
 * time and stored flat on the {@code orders} row — no FK to {@code addresses}.
 * Historical orders always display the original shipping address.
 */
public class ShippingAddressSnapshot {

    private final String name;
    private final String addressLine1;
    private final String addressLine2;
    private final String city;
    private final String state;
    private final String postalCode;
    private final String country;

    public ShippingAddressSnapshot(String name,
                                   String addressLine1,
                                   String addressLine2,
                                   String city,
                                   String state,
                                   String postalCode,
                                   String country) {
        this.name = name;
        this.addressLine1 = addressLine1;
        this.addressLine2 = addressLine2;
        this.city = city;
        this.state = state;
        this.postalCode = postalCode;
        this.country = country;
    }

    public String getName()          { return name; }
    public String getAddressLine1()  { return addressLine1; }
    public String getAddressLine2()  { return addressLine2; }
    public String getCity()          { return city; }
    public String getState()         { return state; }
    public String getPostalCode()    { return postalCode; }
    public String getCountry()       { return country; }
}

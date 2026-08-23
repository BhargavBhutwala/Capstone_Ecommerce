package com.ebookstore.address.dto;

/**
 * Response DTO for an address. Matches the OpenAPI {@code AddressResponse} schema
 * ({@code allOf: [AddressRequest, { id }]}).
 */
public class AddressResponse {

    private final Long id;
    private final String label;
    private final String addressLine1;
    private final String addressLine2;
    private final String city;
    private final String state;
    private final String postalCode;
    private final String country;
    private final boolean isDefault;

    public AddressResponse(Long id,
                           String label,
                           String addressLine1,
                           String addressLine2,
                           String city,
                           String state,
                           String postalCode,
                           String country,
                           boolean isDefault) {
        this.id = id;
        this.label = label;
        this.addressLine1 = addressLine1;
        this.addressLine2 = addressLine2;
        this.city = city;
        this.state = state;
        this.postalCode = postalCode;
        this.country = country;
        this.isDefault = isDefault;
    }

    public Long getId()            { return id; }
    public String getLabel()       { return label; }
    public String getAddressLine1(){ return addressLine1; }
    public String getAddressLine2(){ return addressLine2; }
    public String getCity()        { return city; }
    public String getState()       { return state; }
    public String getPostalCode()  { return postalCode; }
    public String getCountry()     { return country; }
    public boolean isDefault()     { return isDefault; }
}

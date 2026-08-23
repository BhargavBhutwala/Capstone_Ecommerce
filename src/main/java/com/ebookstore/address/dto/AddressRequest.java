package com.ebookstore.address.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for create and update address operations.
 * Validation constraints match the OpenAPI {@code AddressRequest} schema exactly.
 */
public class AddressRequest {

    @Size(max = 50, message = "label must not exceed 50 characters")
    private String label;

    @NotBlank(message = "addressLine1 is required")
    @Size(min = 1, max = 255, message = "addressLine1 must be between 1 and 255 characters")
    private String addressLine1;

    @Size(max = 255, message = "addressLine2 must not exceed 255 characters")
    private String addressLine2;

    @NotBlank(message = "city is required")
    @Size(min = 1, max = 100, message = "city must be between 1 and 100 characters")
    private String city;

    @NotBlank(message = "state is required")
    @Size(min = 1, max = 100, message = "state must be between 1 and 100 characters")
    private String state;

    @NotBlank(message = "postalCode is required")
    @Size(min = 3, max = 20, message = "postalCode must be between 3 and 20 characters")
    private String postalCode;

    @NotBlank(message = "country is required")
    @Size(min = 2, max = 100, message = "country must be between 2 and 100 characters")
    private String country;

    private boolean isDefault = false;

    public AddressRequest() {}

    public String getLabel()         { return label; }
    public void setLabel(String l)   { this.label = l; }

    public String getAddressLine1()              { return addressLine1; }
    public void setAddressLine1(String a)        { this.addressLine1 = a; }

    public String getAddressLine2()              { return addressLine2; }
    public void setAddressLine2(String a)        { this.addressLine2 = a; }

    public String getCity()          { return city; }
    public void setCity(String c)    { this.city = c; }

    public String getState()         { return state; }
    public void setState(String s)   { this.state = s; }

    public String getPostalCode()            { return postalCode; }
    public void setPostalCode(String p)      { this.postalCode = p; }

    public String getCountry()       { return country; }
    public void setCountry(String c) { this.country = c; }

    public boolean isDefault()           { return isDefault; }
    public void setDefault(boolean d)    { this.isDefault = d; }
}

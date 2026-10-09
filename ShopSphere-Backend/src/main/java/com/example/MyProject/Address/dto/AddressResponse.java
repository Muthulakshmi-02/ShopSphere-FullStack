package com.example.MyProject.Address.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddressResponse {

    private Long addressId;
    private String label;
    private String shippingAddress;
    private String city;
    private String state;
    private String zipCode;
    private String phoneNumber;
    private boolean isDefault;

    // See AddressRequest: without the explicit name, the JSON key was "default", so the
    // Angular "Default" badge (addr.isDefault) never appeared.
    @JsonProperty("isDefault")
    public boolean isDefault() {
        return isDefault;
    }

    @JsonProperty("isDefault")
    public void setDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }
}
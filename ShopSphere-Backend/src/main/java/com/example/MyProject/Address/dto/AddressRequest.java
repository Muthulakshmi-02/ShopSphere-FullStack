package com.example.MyProject.Address.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddressRequest {

    @NotBlank(message = "Please give this address a label, e.g. Home or Work.")
    @Size(max = 40, message = "Label must be at most 40 characters.")
    private String label;

    @NotBlank(message = "Street address is required.")
    @Size(max = 255, message = "Street address is too long.")
    private String shippingAddress;

    @NotBlank(message = "City is required.")
    @Size(max = 100, message = "City name is too long.")
    private String city;

    // Required here because checkout (OrderRequest) requires them: a saved address
    // that fails at checkout is useless.
    @NotBlank(message = "State is required.")
    @Size(max = 100, message = "State name is too long.")
    private String state;

    @NotBlank(message = "PIN code is required.")
    @Pattern(regexp = "^[1-9][0-9]{5}$", message = "Please enter a valid 6-digit PIN code.")
    private String zipCode;

    @NotBlank(message = "Phone number is required.")
    @Pattern(regexp = "^(\\+91[\\s-]?)?[6-9](?:[\\s-]?[0-9]){9}$",
            message = "Please enter a valid 10-digit Indian mobile number.")
    private String phoneNumber;

    private boolean isDefault;

    // Why these are written by hand: Lombok turns a boolean field named "isDefault" into
    // isDefault()/setDefault(), and Jackson then reads the JSON key as "default", not
    // "isDefault". The Angular app sends/expects "isDefault", so the flag was silently
    // ignored (this is why the "Default" badge never showed). The explicit names fix it.
    @JsonProperty("isDefault")
    public boolean isDefault() {
        return isDefault;
    }

    @JsonProperty("isDefault")
    public void setDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }
}
package com.example.MyProject.Address.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddressRequest {

    @NotBlank(message = "Please give this address a label, e.g. 'Home'")
    @Size(max = 40)
    private String label;

    @NotBlank(message = "Shipping address is required")
    @Size(max = 255)
    private String shippingAddress;

    @NotBlank(message = "City is required")
    @Size(max = 100)
    private String city;

    @Size(max = 100)
    private String state;

    @Size(max = 20)
    private String zipCode;

    @NotBlank(message = "Phone number is required")
    @Size(max = 20)
    private String phoneNumber;

    private boolean isDefault;
}

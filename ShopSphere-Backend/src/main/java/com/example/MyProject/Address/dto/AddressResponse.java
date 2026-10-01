package com.example.MyProject.Address.dto;

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
}

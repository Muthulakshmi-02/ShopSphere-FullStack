package com.example.MyProject.User.Dto;

import com.example.MyProject.Address.dto.AddressResponse;
import com.example.MyProject.Enum.Role;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileResponse {
    private Long userId;
    private String userName;
    private String email;
    private Role role;
    private long totalOrders;
    private long totalWishlist;
    private int totalAddresses;
    private AddressResponse defaultAddress;
}

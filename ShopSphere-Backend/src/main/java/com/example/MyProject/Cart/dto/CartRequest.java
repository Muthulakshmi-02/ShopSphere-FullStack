package com.example.MyProject.Cart.dto;

import lombok.*;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class CartRequest {
    private Long cartId;
    private List<CartItemRequest> items;
    private Double totalAmount;
}
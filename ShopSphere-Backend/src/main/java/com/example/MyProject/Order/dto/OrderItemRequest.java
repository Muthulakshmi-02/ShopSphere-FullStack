package com.example.MyProject.Order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OrderItemRequest {

    @NotNull(message = "{order.product.id.required}")
    private Long productId;

    @NotNull(message = "{order.quantity.required}")
    @Min(value = 1, message = "{order.quantity.min}")
    private Integer quantity;
}
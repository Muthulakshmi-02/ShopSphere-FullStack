package com.example.MyProject.Order.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OrderRequest {
    @NotBlank(message = "{order.address.required}")
    private String shippingAddress;

    @NotBlank(message = "{order.city.required}")
    private String city;

    @NotBlank(message = "{order.state.required}")
    private String state;

    @NotBlank(message = "{order.zip.required}")
    private String zipCode;

    @NotBlank(message = "{order.phone.required}")
    private String phoneNumber;

    @NotBlank(message = "{order.payment.method}")
    private String paymentMethod;

    private String gatewayToken;
    private String promoCode;

    // --- NEW: Optional fields for Buy Now (Direct Checkout) ---
    private Long productId;
    private Integer quantity;
    private Long variantId;
}
package com.example.MyProject.Order.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class OrderRequest {

    @NotBlank(message = "{order.address.required}")
    @Size(max = 300, message = "Street address is too long.")
    private String shippingAddress;

    @NotBlank(message = "{order.city.required}")
    @Size(max = 100, message = "City name is too long.")
    private String city;

    @NotBlank(message = "{order.state.required}")
    @Size(max = 100, message = "State name is too long.")
    private String state;

    // Same rules as the Angular form, now enforced on the server too (the browser is not trusted).
    @NotBlank(message = "{order.zip.required}")
    @Pattern(regexp = "^[1-9][0-9]{5}$", message = "Please enter a valid 6-digit PIN code.")
    private String zipCode;

    @NotBlank(message = "{order.phone.required}")
    @Pattern(regexp = "^(\\+91[\\s-]?)?[6-9](?:[\\s-]?[0-9]){9}$",
            message = "Please enter a valid 10-digit Indian mobile number.")
    private String phoneNumber;

    // Anything other than COD is treated as an online payment, so only known values are allowed.
    @NotBlank(message = "{order.payment.method}")
    @Pattern(regexp = "CREDIT_CARD|DEBIT_CARD|COD", message = "Unsupported payment method.")
    private String paymentMethod;

    private String gatewayToken;

    @Size(max = 30, message = "Promo code is too long.")
    private String promoCode;

    // Buy Now (direct checkout) fields
    private Long productId;

    @Min(value = 1, message = "Quantity must be at least 1.")
    @Max(value = 99, message = "Quantity cannot be more than 99.")
    private Integer quantity;

    private Long variantId;
}
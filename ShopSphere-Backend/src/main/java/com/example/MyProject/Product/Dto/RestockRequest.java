package com.example.MyProject.Product.Dto;

import jakarta.validation.constraints.Min;
import lombok.*;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RestockRequest {

    @Min(value = 0, message = "Restock quantity cannot be negative.")
    private Integer quantity;

    /**
     * Optional map of variantId -> quantity to add for that variant.
     */
    private Map<Long, Integer> variantRestocks;
}

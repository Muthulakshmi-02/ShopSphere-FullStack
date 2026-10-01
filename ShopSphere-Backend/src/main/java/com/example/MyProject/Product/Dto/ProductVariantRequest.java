package com.example.MyProject.Product.Dto;

import jakarta.validation.constraints.Min;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductVariantRequest {
    // Present (non-null) when editing an existing variant, null when it's
    // a brand new one being added - lets the update logic tell them apart.
    private Long variantId;

    private String size;
    private String color;

    @Min(value = 0, message = "variant.stock.min")
    private Integer stock;

    private Double priceOverride;
    private String sku;
}

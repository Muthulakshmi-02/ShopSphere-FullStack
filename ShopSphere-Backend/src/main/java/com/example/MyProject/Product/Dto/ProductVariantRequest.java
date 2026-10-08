package com.example.MyProject.Product.Dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductVariantRequest {

    // Present (non-null) when editing an existing variant, null for a brand new one.
    private Long variantId;

    // Limits match the database columns (30 / 30 / 60); longer values used to fail with a 500.
    @Size(max = 30, message = "Size must be at most 30 characters.")
    private String size;

    @Size(max = 30, message = "Colour must be at most 30 characters.")
    private String color;

    @Min(value = 0, message = "{variant.stock.min}")
    @Max(value = 1000000, message = "Variant stock is too high.")
    private Integer stock;

    // null = use the product's base price; otherwise it must be a real price.
    @DecimalMin(value = "0.01", message = "Price override must be at least 0.01.")
    private Double priceOverride;

    @Size(max = 60, message = "SKU must be at most 60 characters.")
    private String sku;
}
package com.example.MyProject.Product.Dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductVariantResponse {
    private Long variantId;
    private String size;
    private String color;
    private Integer stock;
    private Double priceOverride;
    private String sku;
}

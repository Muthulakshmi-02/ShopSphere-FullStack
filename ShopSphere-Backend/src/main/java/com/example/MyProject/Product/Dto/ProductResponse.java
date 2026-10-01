package com.example.MyProject.Product.Dto;

import com.example.MyProject.Enum.ProductStatus;
import lombok.*;
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductResponse {
    private Long productId;
    private String name;
    private String description;
    private Double price;
    private Integer stock;
    private String imageUrl;
    private ProductStatus status;
    private Double rating;
    private Integer reviewCount;
    private Integer discountPercentage;

    private Long categoryId;
    private String categoryName;

    @Builder.Default
    private java.util.List<ProductVariantResponse> variants = new java.util.ArrayList<>();
}

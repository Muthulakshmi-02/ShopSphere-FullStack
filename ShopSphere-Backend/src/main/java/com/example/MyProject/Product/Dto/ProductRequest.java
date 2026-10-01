package com.example.MyProject.Product.Dto;

import com.example.MyProject.Enum.ProductStatus;
import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductRequest {

    @NotBlank(message = "product.name.required")
    @Size(min = 3, max = 100, message = "product.name.size")
    private String name;

    @NotBlank(message = "product.description.required")
    @Size(max = 500, message = "product.description.size")
    private String description;

    @DecimalMin(value = "0.01", message = "product.price.min")
    private Double price;

    @Min(value = 0, message = "product.stock.min")
    private Integer stock;

    @NotBlank(message = "product.image.required")
    @Size(max = 500, message = "product.image.size")
    private String imageUrl;

    @NotNull(message = "product.status.required")
    private ProductStatus status;

    @DecimalMin(value = "0.0", message = "product.rating.min")
    @DecimalMax(value = "5.0", message = "product.rating.max")
    private Double rating;

    @Min(value = 0, message = "product.reviewCount.min")
    private Integer reviewCount;

    @Min(value = 0, message = "product.discount.min")
    @Max(value = 90, message = "product.discount.max")
    private Integer discountPercentage;

    @NotNull(message = "Category ID is required")
    private Long categoryId;

    // Optional - if provided (non-empty), replaces this product's entire
    // variant set. If omitted/empty, the product stays (or becomes) a
    // simple product sold via price/stock directly.
    @Builder.Default
    private java.util.List<ProductVariantRequest> variants = new java.util.ArrayList<>();
}

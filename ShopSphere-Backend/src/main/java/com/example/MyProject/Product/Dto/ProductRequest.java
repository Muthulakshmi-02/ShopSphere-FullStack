package com.example.MyProject.Product.Dto;

import com.example.MyProject.Enum.ProductStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductRequest {

    // {braces} make Spring look the text up in messages.properties.
    // Without them the admin would literally see "product.name.required".
    // (Use the corrected messages.properties: it adds product.rating.*, product.discount.*, etc.)
    @NotBlank(message = "{product.name.required}")
    @Size(min = 3, max = 100, message = "{product.name.size}")
    private String name;

    @NotBlank(message = "{product.description.required}")
    @Size(max = 500, message = "{product.description.size}")
    private String description;

    // @NotNull added: @DecimalMin ignores null, so a missing price slipped through and
    // crashed later with a 500. Price is always required (variant products use it as the base).
    @NotNull(message = "Price is required.")
    @DecimalMin(value = "0.01", message = "{product.price.min}")
    @DecimalMax(value = "10000000", message = "Price is too high.")
    private Double price;

    // Not @NotNull on purpose: for products WITH variants the stock is recalculated as the
    // sum of the variants. A missing value is treated as 0 (see the ProductService patch).
    @Min(value = 0, message = "{product.stock.min}")
    @Max(value = 1000000, message = "Stock is too high.")
    private Integer stock;

    @NotBlank(message = "{product.image.required}")
    @Size(max = 500, message = "{product.image.size}")
    private String imageUrl;

    // IGNORED by the server: status is always derived from stock (OUT_OF_STOCK at 0).
    // It used to be @NotNull, which forced clients to send a value that did nothing.
    // To stop selling a product, set its stock to 0.
    private ProductStatus status;

    @DecimalMin(value = "0.0", message = "{product.rating.min}")
    @DecimalMax(value = "5.0", message = "{product.rating.max}")
    private Double rating;

    @Min(value = 0, message = "{product.reviewCount.min}")
    private Integer reviewCount;

    @Min(value = 0, message = "{product.discount.min}")
    @Max(value = 90, message = "{product.discount.max}")
    private Integer discountPercentage;

    @NotNull(message = "Category ID is required")
    private Long categoryId;

    // @Valid added: without it the rules on ProductVariantRequest never ran.
    // Optional - if provided (non-empty), replaces this product's entire variant set.
    @Valid
    @Size(max = 50, message = "A product can have at most 50 variants.")
    @Builder.Default
    private java.util.List<ProductVariantRequest> variants = new java.util.ArrayList<>();
}
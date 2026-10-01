package com.example.MyProject.Models;

import com.example.MyProject.Enum.ProductStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Long productId;

    @NotBlank(message = "product.name.required")
    @Size(min = 3, max = 100, message = "product.name.size")
    @Column(nullable = false, length = 100)
    private String name;

    @NotBlank(message = "product.description.required")
    @Column(nullable = false, length = 500)
    private String description;

    @Column(nullable = false)
    private Double price;

    @Min(value = 0, message = "product.stock.min")
    @Column(nullable = false)
    private Integer stock;

    @NotBlank(message = "product.image.required")
    @Column(nullable = false, length = 500)
    private String imageUrl;

    // Admin-set display fields (stars / "X% off" badge on product cards).
    @DecimalMin(value = "0.0", message = "product.rating.min")
    @DecimalMax(value = "5.0", message = "product.rating.max")
    @Column
    private Double rating;

    @Min(value = 0, message = "product.reviewCount.min")
    @Column
    private Integer reviewCount;

    @Min(value = 0, message = "product.discount.min")
    @Max(value = 90, message = "product.discount.max")
    @Column(name = "discount_percentage")
    private Integer discountPercentage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProductStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    // Optional - empty list means a simple product sold using price/stock above.
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ProductVariant> variants = new ArrayList<>();

    @OneToMany(mappedBy = "product")
    @Builder.Default
    private List<CartItem> cartItems = new ArrayList<>();

    // NO cascade / orphanRemoval here: order history must outlive the product.
    // (It used to be CascadeType.ALL + orphanRemoval, which let deleting a product
    // delete the order lines of past customers' orders.)
    @OneToMany(mappedBy = "product")
    @Builder.Default
    private List<OrderItem> orderItems = new ArrayList<>();

}
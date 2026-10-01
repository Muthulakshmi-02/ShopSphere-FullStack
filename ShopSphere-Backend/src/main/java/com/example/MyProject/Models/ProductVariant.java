package com.example.MyProject.Models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import lombok.*;

/**
 * A specific size/color combination of a product, with its own stock.
 * Deliberately optional at the Product level - a product with zero
 * variants behaves exactly as before (uses Product.price/Product.stock
 * directly). A product WITH variants is expected to be sold only through
 * its variants; the base product's own stock/price become display
 * fallbacks rather than what's actually sellable. This keeps every
 * existing product working unchanged while letting new products opt into
 * real size/color tracking.
 */
@Entity
@Table(name = "product_variants")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long variantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    @JsonIgnore
    private Product product;

    // Both nullable - a variant might only differ by size (e.g. shoes) or
    // only by color (e.g. a single-size accessory), or both (apparel).
    @Column(length = 30)
    private String size;

    @Column(length = 30)
    private String color;

    @Min(value = 0, message = "variant.stock.min")
    @Column(nullable = false)
    private Integer stock;

    // Null means "use the product's base price" - most variants won't need
    // a price override, only the rare case where e.g. a larger size costs more.
    @Column(name = "price_override")
    private Double priceOverride;

    @Column(length = 60)
    private String sku;
}

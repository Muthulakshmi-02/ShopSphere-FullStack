package com.example.MyProject.Models;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
// @Data was replaced by @Getter/@Setter. @Data also generates equals/hashCode/toString over
// every field, including the lazy order/product/variant relations; on JPA entities that can
// trigger unintended lazy loading and unpredictable collection behaviour.
@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long orderItemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    // Nullable - set when the purchased item was a specific variant.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id")
    private ProductVariant variant;

    // Snapshot of the variant's size/color at time of purchase.
    @Column(name = "variant_label", length = 100)
    private String variantLabel;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private Double price;
}
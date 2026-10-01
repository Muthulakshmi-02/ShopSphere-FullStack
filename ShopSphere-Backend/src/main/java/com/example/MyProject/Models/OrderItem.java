package com.example.MyProject.Models;
import com.example.MyProject.Models.Order;
import com.example.MyProject.Models.Product;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
@Entity
@Table(name = "order_items")
@Data
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

    // Snapshot of the variant's size/color at time of purchase (e.g.
    // "Size: 9, Color: Black") so order history still displays correctly
    // even if the variant itself is later removed from the product.
    @Column(name = "variant_label", length = 100)
    private String variantLabel;

    @Column(nullable = false)
    private Integer quantity;
    @Column(nullable = false)
    private Double price;
}
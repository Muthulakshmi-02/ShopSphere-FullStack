package com.example.MyProject.Models;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_details")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long paymentId;

    // One order has one payment record. PaymentService updates this row instead of inserting a
    // second one (a second insert for the same order breaks the unique order_id column).
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    // Gateway payment id; vital for finding the transaction in the Razorpay dashboard.
    @Column(name = "transaction_id", unique = true, nullable = false)
    private String transactionId;

    // e.g. "RAZORPAY"
    @Column(nullable = false)
    private String paymentGateway;

    // e.g. "CREDIT_CARD", "CARD", "UPI", "NETBANKING"
    @Column(nullable = false)
    private String paymentMethod;

    // "COMPLETED" or "REFUNDED"
    @Column(nullable = false)
    private String status;

    // The amount actually charged (protects against later price changes in the Order table).
    @Column(nullable = false)
    private Double amount;

    @Size(min = 3, max = 3, message = "Currency code must be exactly 3 characters")
    @Column(name = "currency", length = 3, nullable = false)
    @Builder.Default
    private String currency = "INR";

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    // Replaces @CreatedDate / @LastModifiedDate: those only work when @EnableJpaAuditing is on
    // your application class, otherwise both columns stay null.
    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
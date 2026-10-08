package com.example.MyProject.Models;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "payment_details")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class) // Tracks dates automatically
public class PaymentDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long paymentId;

    /**
     * OneToOne because one order usually has one specific payment record.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    /**
     * The unique ID provided by the payment gateway (e.g., Razorpay/Stripe/PayPal).
     * This is vital for searching transactions in your provider's dashboard.
     */
    @Column(name = "transaction_id", unique = true, nullable = false)
    private String transactionId;

    /**
     * Examples: "STRIPE", "RAZORPAY", "PAYPAL"
     */
    @Column(nullable = false)
    private String paymentGateway;

    /**
     * Examples: "CREDIT_CARD", "UPI", "NET_BANKING", "WALLET"
     */
    @Column(nullable = false)
    private String paymentMethod;

    /**
     * Current state of the payment.
     * Examples: "PENDING", "COMPLETED", "FAILED", "REFUNDED"
     */
    @Column(nullable = false)
    private String status;

    /**
     * The actual amount charged (to protect against price changes in the Order table).
     */
    @Column(nullable = false)
    private Double amount;

    /**
     * Capture the currency for international stores.
     */
    @Size(min = 3, max = 3, message = "Currency code must be exactly 3 characters")
    @Column(name = "currency", length = 3, nullable = false)
    @Builder.Default
    private String currency = "INR";

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}

//package com.example.MyProject.Models;
//
//import com.example.MyProject.Enum.OrderStatus;
//import com.example.MyProject.Enum.PaymentStatus;
//import jakarta.persistence.*;
//import jakarta.validation.constraints.NotBlank;
//import lombok.*;
//import java.time.LocalDateTime;
//import java.util.List;
//@Entity
//@Table(name = "orders")
//@Getter
//@Setter
//@NoArgsConstructor
//@AllArgsConstructor
//public class Order {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long orderId;
//
//    @ManyToOne
//    @JoinColumn(name = "user_id", nullable = false)
//    private User user;
//
//    @Column(nullable = false)
//    private Double totalAmount;
//
//    @Column(nullable = false)
//    private LocalDateTime orderDate;
//
//    @Column(nullable = false)
//    private LocalDateTime createdAt;
//
//    @Enumerated(EnumType.STRING)
//    private OrderStatus orderStatus;
//
//    @Column(nullable = false)
//    private PaymentStatus paymentStatus;
//
//    @NotBlank( message="order.payment.method")
//    private String paymentMethod;
//
//    @Column(nullable = false)
//    @NotBlank( message="order.phone.required")
//    private String phoneNumber;
//
//    @Column(columnDefinition = "TEXT", nullable = false)
//    @NotBlank( message="order.address.required")
//    private String shippingAddress;
//
//    @Column(nullable = false)
//    @NotBlank( message="order.city.required")
//    private String city;
//
//    @Column(nullable = false)
//    @NotBlank( message="order.state.required")
//    private String state;
//
//    @Column( nullable = false)
//   @NotBlank( message="order.zip.required")
//    private String zipCode;
//
//    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
//    private List<OrderItem> orderItems;
//}

package com.example.MyProject.Models;

import com.example.MyProject.Enum.OrderStatus;
import com.example.MyProject.Enum.PaymentStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long orderId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Double totalAmount;

    @Column(nullable = false)
    private LocalDateTime orderDate;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus orderStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus paymentStatus;

    @NotBlank(message = "order.payment.method")
    private String paymentMethod;

    @Column(nullable = false)
    @NotBlank(message = "order.phone.required")
    private String phoneNumber;

    @Column(columnDefinition = "TEXT", nullable = false)
    @NotBlank(message = "order.address.required")
    private String shippingAddress;

    @Column(nullable = false)
    @NotBlank(message = "order.city.required")
    private String city;

    @Column(nullable = false)
    @NotBlank(message = "order.state.required")
    private String state;

    @Column(nullable = false)
    @NotBlank(message = "order.zip.required")
    private String zipCode;

    // Razorpay Gateway Order and Payment Tracking
    @Column(name = "razorpay_order_id")
    private String razorpayOrderId;

    @Column(name = "razorpay_payment_id")
    private String razorpayPaymentId;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> orderItems;

    @Column(name = "promo_code")
    private String promoCode;
    
    @Builder.Default
    @Column(name = "discount_amount")
    private Double discountAmount = 0.0;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.orderDate == null) {
            this.orderDate = LocalDateTime.now();
        }
    }
}
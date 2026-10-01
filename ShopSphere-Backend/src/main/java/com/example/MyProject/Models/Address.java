package com.example.MyProject.Models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Entity
@Table(name = "addresses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long addressId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    // A short name the customer gives it, e.g. "Home", "Work" - purely for
    // their own recognition when picking between saved addresses.
    @NotBlank
    @Column(nullable = false, length = 40)
    private String label;

    @NotBlank
    @Column(nullable = false, length = 255)
    private String shippingAddress;

    @NotBlank
    @Column(nullable = false, length = 100)
    private String city;

    @Column(length = 100)
    private String state;

    @Column(length = 20)
    private String zipCode;

    @NotBlank
    @Column(nullable = false, length = 20)
    private String phoneNumber;

    @Builder.Default
    @Column(nullable = false)
    private boolean isDefault = false;
}

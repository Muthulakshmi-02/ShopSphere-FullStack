package com.example.MyProject.Order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * A single "someone just bought something" entry for the storefront's live
 * ticker. Deliberately minimal - no order ID, no email, no address - this
 * is public-facing social proof, not order data.
 */
@Getter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class RecentActivityResponse {
    private String buyerName;
    private String productName;
}

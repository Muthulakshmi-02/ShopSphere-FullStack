package com.example.MyProject.Services;

import com.example.MyProject.Enum.OrderStatus;
import com.example.MyProject.Enum.PaymentStatus;
import com.example.MyProject.Models.Order;
import com.example.MyProject.Models.OrderItem;
import com.example.MyProject.Models.Product;
import com.example.MyProject.Models.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InvoicePdfServiceTest {

    private InvoicePdfService invoicePdfService;

    @BeforeEach
    void setUp() {
        invoicePdfService = new InvoicePdfService();
    }

    @Test
    @DisplayName("Should generate valid non-empty PDF document starting with %PDF- header")
    void testGenerateInvoicePdf() {
        User user = User.builder()
                .userName("Rahul Sharma")
                .email("rahul@example.com")
                .build();

        Product product = Product.builder()
                .name("Wireless Noise Cancelling Headphones")
                .price(2999.0)
                .build();

        OrderItem item = new OrderItem();
        item.setProduct(product);
        item.setQuantity(2);
        item.setPrice(2999.0);
        item.setVariantLabel("Color: Midnight Black");

        Order order = Order.builder()
                .orderId(101L)
                .user(user)
                .orderDate(LocalDateTime.now())
                .orderStatus(OrderStatus.CONFIRMED)
                .paymentStatus(PaymentStatus.PAID)
                .paymentMethod("RAZORPAY")
                .phoneNumber("+91 9876543210")
                .shippingAddress("123 MG Road, Indiranagar")
                .city("Bengaluru")
                .state("Karnataka")
                .zipCode("560038")
                .orderItems(List.of(item))
                .promoCode("SAVE10")
                .discountAmount(599.80)
                .totalAmount(5398.20)
                .build();

        byte[] pdfBytes = invoicePdfService.generateInvoice(order);

        assertNotNull(pdfBytes, "PDF byte array should not be null");
        assertTrue(pdfBytes.length > 500, "PDF document should contain content");

        // Verify standard PDF magic header bytes (%PDF-)
        assertEquals((byte) '%', pdfBytes[0]);
        assertEquals((byte) 'P', pdfBytes[1]);
        assertEquals((byte) 'D', pdfBytes[2]);
        assertEquals((byte) 'F', pdfBytes[3]);
        assertEquals((byte) '-', pdfBytes[4]);
    }
}

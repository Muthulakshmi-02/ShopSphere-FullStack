//package com.example.MyProject.Services;
//
//import com.example.MyProject.Enum.OrderStatus;
//import com.example.MyProject.Models.Order;
//import lombok.RequiredArgsConstructor;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.mail.MailException;
//import org.springframework.mail.javamail.JavaMailSender;
//import org.springframework.mail.javamail.MimeMessageHelper;
//import org.springframework.stereotype.Service;
//
//import jakarta.mail.internet.MimeMessage;
//import java.util.Locale;
//
///**
// * Sends order-related emails (confirmation, status updates, cancellation).
// *
// * Deliberately fails soft, not hard: a broken SMTP config or a temporary
// * network issue should never break someone's checkout or an admin's status
// * update - the order itself is the source of truth, the email is a
// * courtesy on top of it. Every send is wrapped so a failure just logs a
// * warning instead of propagating an exception back to the caller.
// *
// * Also respects app.mail.enabled (defaults to false) - until real SMTP
// * credentials are configured, this skips sending entirely rather than
// * repeatedly failing against placeholder credentials.
// */
//@Service
//@RequiredArgsConstructor
//public class EmailService {
//
//    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
//
//    private final JavaMailSender mailSender;
//
//    @Value("${app.mail.enabled:false}")
//    private boolean mailEnabled;
//
//    @Value("${app.mail.from-name:Your Store}")
//    private String fromName;
//
//    @Value("${spring.mail.username:}")
//    private String fromAddress;
//
//    public void sendOrderConfirmation(Order order) {
//        String subject = "Order Confirmed - #" + order.getOrderId();
//        String body = buildOrderEmail(
//                order,
//                "Thanks for your order!",
//                "We've received your order and it's now being processed. Here's a summary:"
//        );
//        send(order.getUser().getEmail(), subject, body);
//    }
//
//    public void sendOrderStatusUpdate(Order order) {
//        String friendlyStatus = order.getOrderStatus().toString().toLowerCase(Locale.ROOT);
//        String subject = "Order #" + order.getOrderId() + " - " + capitalize(friendlyStatus);
//
//        String headline;
//        String message;
//        switch (order.getOrderStatus()) {
//            case SHIPPED -> {
//                headline = "Your order is on its way!";
//                message = "Order #" + order.getOrderId() + " has shipped and is headed your way.";
//            }
//            case DELIVERED -> {
//                headline = "Your order has been delivered";
//                message = "Order #" + order.getOrderId() + " was marked as delivered. We hope you love it!";
//            }
//            case CONFIRMED -> {
//                headline = "Your order has been confirmed";
//                message = "Order #" + order.getOrderId() + " has been confirmed and is being prepared.";
//            }
//            default -> {
//                headline = "Order status update";
//                message = "Order #" + order.getOrderId() + " status is now: " + capitalize(friendlyStatus) + ".";
//            }
//        }
//
//        String body = buildOrderEmail(order, headline, message);
//        send(order.getUser().getEmail(), subject, body);
//    }
//
//    public void sendOrderCancelled(Order order) {
//        String subject = "Order #" + order.getOrderId() + " - Cancelled";
//        String body = buildOrderEmail(
//                order,
//                "Your order has been cancelled",
//                "Order #" + order.getOrderId() + " has been cancelled and any charge will be refunded " +
//                        "according to your payment method's standard timeline."
//        );
//        send(order.getUser().getEmail(), subject, body);
//    }
//
//    // --- Internals ---
//
//    private void send(String to, String subject, String htmlBody) {
//        if (!mailEnabled) {
//            log.info("Email sending is disabled (app.mail.enabled=false) - skipped '{}' to {}", subject, to);
//            return;
//        }
//        if (to == null || to.isBlank()) {
//            log.warn("Skipped sending '{}' - recipient email is missing", subject);
//            return;
//        }
//
//        try {
//            MimeMessage message = mailSender.createMimeMessage();
//            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
//            helper.setTo(to);
//            helper.setSubject(subject);
//            helper.setText(htmlBody, true);
//            if (fromAddress != null && !fromAddress.isBlank()) {
//                helper.setFrom(fromAddress, fromName);
//            }
//            mailSender.send(message);
//            log.info("Sent email '{}' to {}", subject, to);
//        } catch (MailException | java.io.UnsupportedEncodingException e) {
//            // Never let an email failure break the actual order operation
//            // that triggered it - log and move on.
//            log.warn("Failed to send email '{}' to {}: {}", subject, to, e.getMessage());
//        }
//    }
//
//    /**
//     * Simple, self-contained HTML email - deliberately plain/table-free
//     * beyond basic inline styling, since email clients render CSS
//     * inconsistently. Good enough for a small store; swap for a proper
//     * template engine (Thymeleaf) later if emails need to get fancier.
//     */
//    private String buildOrderEmail(Order order, String headline, String message) {
//        StringBuilder itemsHtml = new StringBuilder();
//        if (order.getOrderItems() != null) {
//            for (var item : order.getOrderItems()) {
//                String name = item.getProduct() != null ? item.getProduct().getName() : "Item";
//                itemsHtml.append("<tr>")
//                        .append("<td style=\"padding:8px 0;border-bottom:1px solid #2a2a2a;color:#ddd;\">")
//                        .append(name);
//                if (item.getVariantLabel() != null && !item.getVariantLabel().isBlank()) {
//                    itemsHtml.append(" <span style=\"color:#888;\">(").append(item.getVariantLabel()).append(")</span>");
//                }
//                itemsHtml.append("</td>")
//                        .append("<td style=\"padding:8px 0;border-bottom:1px solid #2a2a2a;color:#ddd;text-align:center;\">x")
//                        .append(item.getQuantity())
//                        .append("</td>")
//                        .append("<td style=\"padding:8px 0;border-bottom:1px solid #2a2a2a;color:#ddd;text-align:right;\">$")
//                        .append(String.format("%.2f", item.getPrice() * item.getQuantity()))
//                        .append("</td>")
//                        .append("</tr>");
//            }
//        }
//
//        return "<div style=\"font-family:Arial,sans-serif;background:#0B0F1A;padding:32px;\">"
//                + "<div style=\"max-width:520px;margin:0 auto;background:#161D2E;border-radius:16px;padding:32px;color:#F5F5F7;\">"
//                + "<h1 style=\"font-size:20px;margin:0 0 8px;color:#F5F5F7;\">" + headline + "</h1>"
//                + "<p style=\"color:#8B92A8;font-size:14px;line-height:1.6;margin:0 0 24px;\">" + message + "</p>"
//                + "<table style=\"width:100%;border-collapse:collapse;font-size:14px;\">"
//                + itemsHtml
//                + "</table>"
//                + "<div style=\"display:flex;justify-content:space-between;margin-top:16px;padding-top:16px;border-top:1px solid #2a2a2a;\">"
//                + "<strong style=\"color:#F5F5F7;\">Total</strong>"
//                + "<strong style=\"color:#FF3D71;\">$" + String.format("%.2f", order.getTotalAmount()) + "</strong>"
//                + "</div>"
//                + "<p style=\"color:#6E7793;font-size:12px;margin-top:32px;\">"
//                + "Shipping to: " + order.getShippingAddress() + ", " + order.getCity()
//                + "</p>"
//                + "</div>"
//                + "</div>";
//    }
//
//    private String capitalize(String s) {
//        if (s == null || s.isEmpty()) return s;
//        return s.substring(0, 1).toUpperCase(Locale.ROOT) + s.substring(1);
//    }
//}
package com.example.MyProject.Services;

import com.example.MyProject.Enum.OrderStatus;
import com.example.MyProject.Models.Order;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.util.Locale;

/**
 * Sends order-related emails (confirmation, status updates, cancellation).
 *
 * Deliberately fails soft, not hard: a broken SMTP config or a temporary
 * network issue should never break someone's checkout or an admin's status
 * update - the order itself is the source of truth, the email is a
 * courtesy on top of it. Every send is wrapped so a failure just logs a
 * warning instead of propagating an exception back to the caller.
 *
 * Also respects app.mail.enabled (defaults to false) - until real SMTP
 * credentials are configured, this skips sending entirely rather than
 * repeatedly failing against placeholder credentials.
 */
@Service
@RequiredArgsConstructor
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    @Value("${app.mail.from-name:Your Store}")
    private String fromName;

    @Value("${spring.mail.username:}")
    private String fromAddress;

    public void sendOrderConfirmation(Order order) {
        String subject = "Order Confirmed - #" + order.getOrderId();
        String body = buildOrderEmail(
                order,
                "Thanks for your order!",
                "We've received your order and it's now being processed. Here's a summary:"
        );
        send(order.getUser().getEmail(), subject, body);
    }

    public void sendOrderStatusUpdate(Order order) {
        String friendlyStatus = order.getOrderStatus().toString().toLowerCase(Locale.ROOT);
        String subject = "Order #" + order.getOrderId() + " - " + capitalize(friendlyStatus);

        String headline;
        String message;
        switch (order.getOrderStatus()) {
            case SHIPPED -> {
                headline = "Your order is on its way!";
                message = "Order #" + order.getOrderId() + " has shipped and is headed your way.";
            }
            case DELIVERED -> {
                headline = "Your order has been delivered";
                message = "Order #" + order.getOrderId() + " was marked as delivered. We hope you love it!";
            }
            case CONFIRMED -> {
                headline = "Your order has been confirmed";
                message = "Order #" + order.getOrderId() + " has been confirmed and is being prepared.";
            }
            default -> {
                headline = "Order status update";
                message = "Order #" + order.getOrderId() + " status is now: " + capitalize(friendlyStatus) + ".";
            }
        }

        String body = buildOrderEmail(order, headline, message);
        send(order.getUser().getEmail(), subject, body);
    }

    public void sendOrderCancelled(Order order) {
        String subject = "Order #" + order.getOrderId() + " - Cancelled";
        String body = buildOrderEmail(
                order,
                "Your order has been cancelled",
                "Order #" + order.getOrderId() + " has been cancelled and any charge will be refunded " +
                        "according to your payment method's standard timeline."
        );
        send(order.getUser().getEmail(), subject, body);
    }

    // --- Internals ---

    private void send(String to, String subject, String htmlBody) {
        if (!mailEnabled) {
            log.info("Email sending is disabled (app.mail.enabled=false) - skipped '{}' to {}", subject, to);
            return;
        }
        if (to == null || to.isBlank()) {
            log.warn("Skipped sending '{}' - recipient email is missing", subject);
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            if (fromAddress != null && !fromAddress.isBlank()) {
                helper.setFrom(fromAddress, fromName);
            }
            mailSender.send(message);
            log.info("Sent email '{}' to {}", subject, to);
        } catch (MailException | MessagingException | UnsupportedEncodingException e) {
            // Never let an email failure break the actual order operation
            // that triggered it - log and move on.
            log.warn("Failed to send email '{}' to {}: {}", subject, to, e.getMessage());
        }
    }

    /**
     * Simple, self-contained HTML email - deliberately plain/table-free
     * beyond basic inline styling, since email clients render CSS
     * inconsistently. Good enough for a small store; swap for a proper
     * template engine (Thymeleaf) later if emails need to get fancier.
     */
    private String buildOrderEmail(Order order, String headline, String message) {
        StringBuilder itemsHtml = new StringBuilder();
        if (order.getOrderItems() != null) {
            for (var item : order.getOrderItems()) {
                String name = item.getProduct() != null ? item.getProduct().getName() : "Item";
                itemsHtml.append("<tr>")
                        .append("<td style=\"padding:8px 0;border-bottom:1px solid #2a2a2a;color:#ddd;\">")
                        .append(name);
                if (item.getVariantLabel() != null && !item.getVariantLabel().isBlank()) {
                    itemsHtml.append(" <span style=\"color:#888;\">(").append(item.getVariantLabel()).append(")</span>");
                }
                itemsHtml.append("</td>")
                        .append("<td style=\"padding:8px 0;border-bottom:1px solid #2a2a2a;color:#ddd;text-align:center;\">x")
                        .append(item.getQuantity())
                        .append("</td>")
                        .append("<td style=\"padding:8px 0;border-bottom:1px solid #2a2a2a;color:#ddd;text-align:right;\">$")
                        .append(String.format("%.2f", item.getPrice() * item.getQuantity()))
                        .append("</td>")
                        .append("</tr>");
            }
        }

        return "<div style=\"font-family:Arial,sans-serif;background:#0B0F1A;padding:32px;\">"
                + "<div style=\"max-width:520px;margin:0 auto;background:#161D2E;border-radius:16px;padding:32px;color:#F5F5F7;\">"
                + "<h1 style=\"font-size:20px;margin:0 0 8px;color:#F5F5F7;\">" + headline + "</h1>"
                + "<p style=\"color:#8B92A8;font-size:14px;line-height:1.6;margin:0 0 24px;\">" + message + "</p>"
                + "<table style=\"width:100%;border-collapse:collapse;font-size:14px;\">"
                + itemsHtml
                + "</table>"
                + "<div style=\"display:flex;justify-content:space-between;margin-top:16px;padding-top:16px;border-top:1px solid #2a2a2a;\">"
                + "<strong style=\"color:#F5F5F7;\">Total</strong>"
                + "<strong style=\"color:#FF3D71;\">$" + String.format("%.2f", order.getTotalAmount()) + "</strong>"
                + "</div>"
                + "<p style=\"color:#6E7793;font-size:12px;margin-top:32px;\">"
                + "Shipping to: " + order.getShippingAddress() + ", " + order.getCity()
                + "</p>"
                + "</div>"
                + "</div>";
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase(Locale.ROOT) + s.substring(1);
    }
}

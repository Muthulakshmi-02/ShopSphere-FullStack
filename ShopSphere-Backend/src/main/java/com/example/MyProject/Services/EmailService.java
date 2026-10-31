
// package com.example.MyProject.Services;

// import com.example.MyProject.Enum.PaymentStatus;
// import com.example.MyProject.Models.Order;
// import lombok.RequiredArgsConstructor;
// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;
// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.mail.MailException;
// import org.springframework.mail.javamail.JavaMailSender;
// import org.springframework.mail.javamail.MimeMessageHelper;
// import org.springframework.stereotype.Service;
// import org.springframework.web.util.HtmlUtils;

// import jakarta.mail.MessagingException;
// import jakarta.mail.internet.MimeMessage;
// import java.io.UnsupportedEncodingException;
// import java.util.Locale;

// /**
//  * Sends order emails. Fails soft: a mail problem is logged, never thrown.
//  * Called from OrderEmailListener AFTER the order transaction commits, on a
//  * background thread. Respects app.mail.enabled (default false).
//  */
// @Service
// @RequiredArgsConstructor
// public class EmailService {

//     private static final Logger log = LoggerFactory.getLogger(EmailService.class);

//     private final JavaMailSender mailSender;

//     @Value("${app.mail.enabled:false}")
//     private boolean mailEnabled;

//     @Value("${app.mail.from-name:ShopSphere}")
//     private String fromName;

//     @Value("${spring.mail.username:}")
//     private String fromAddress;

//     public void sendOrderConfirmation(Order order) {
//         String subject = "Order Confirmed - #" + order.getOrderId();
//         String body = buildOrderEmail(order,
//                 "Thanks for your order!",
//                 "We've received your order and it's now being processed. Here's a summary:");
//         send(order.getUser().getEmail(), subject, body);
//     }

//     public void sendOrderStatusUpdate(Order order) {
//         String friendlyStatus = order.getOrderStatus().toString().toLowerCase(Locale.ROOT);
//         String subject = "Order #" + order.getOrderId() + " - " + capitalize(friendlyStatus);

//         String headline;
//         String message;
//         switch (order.getOrderStatus()) {
//             case SHIPPED -> {
//                 headline = "Your order is on its way!";
//                 message = "Order #" + order.getOrderId() + " has shipped and is headed your way.";
//             }
//             case DELIVERED -> {
//                 headline = "Your order has been delivered";
//                 message = "Order #" + order.getOrderId() + " was marked as delivered. We hope you love it!";
//             }
//             case CONFIRMED -> {
//                 headline = "Your order has been confirmed";
//                 message = "Order #" + order.getOrderId() + " has been confirmed and is being prepared.";
//             }
//             default -> {
//                 headline = "Order status update";
//                 message = "Order #" + order.getOrderId() + " status is now: " + capitalize(friendlyStatus) + ".";
//             }
//         }
//         send(order.getUser().getEmail(), subject, buildOrderEmail(order, headline, message));
//     }

//     public void sendOrderCancelled(Order order) {
//         String subject = "Order #" + order.getOrderId() + " - Cancelled";

//         // Only promise a refund if money was actually taken.
//         String message = (order.getPaymentStatus() == PaymentStatus.REFUND_PENDING)
//                 ? "Order #" + order.getOrderId() + " has been cancelled. Your payment will be refunded to your "
//                   + "original payment method; once processed it can take a few business days to appear."
//                 : "Order #" + order.getOrderId() + " has been cancelled. No payment was taken for this order.";

//         send(order.getUser().getEmail(), subject,
//                 buildOrderEmail(order, "Your order has been cancelled", message));
//     }

//     // --- Internals ---

//     private void send(String to, String subject, String htmlBody) {
//         if (!mailEnabled) {
//             log.info("Email sending is disabled (app.mail.enabled=false) - skipped '{}' to {}", subject, to);
//             return;
//         }
//         if (to == null || to.isBlank()) {
//             log.warn("Skipped sending '{}' - recipient email is missing", subject);
//             return;
//         }

//         try {
//             MimeMessage message = mailSender.createMimeMessage();
//             MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
//             helper.setTo(to);
//             helper.setSubject(subject);
//             helper.setText(htmlBody, true);
//             if (fromAddress != null && !fromAddress.isBlank()) {
//                 helper.setFrom(fromAddress, fromName);
//             }
//             mailSender.send(message);
//             log.info("Sent email '{}' to {}", subject, to);
//         } catch (MailException | MessagingException | UnsupportedEncodingException e) {
//             log.warn("Failed to send email '{}' to {}: {}", subject, to, e.getMessage());
//         }
//     }

//     private String buildOrderEmail(Order order, String headline, String message) {
//         StringBuilder rows = new StringBuilder();
//         if (order.getOrderItems() != null) {
//             for (var item : order.getOrderItems()) {
//                 String name = item.getProduct() != null ? item.getProduct().getName() : "Item";
//                 rows.append("<tr>")
//                         .append("<td style=\"padding:8px 0;border-bottom:1px solid #2a2a2a;color:#ddd;\">")
//                         .append(esc(name));
//                 if (item.getVariantLabel() != null && !item.getVariantLabel().isBlank()) {
//                     rows.append(" <span style=\"color:#888;\">(").append(esc(item.getVariantLabel())).append(")</span>");
//                 }
//                 rows.append("</td>")
//                         .append("<td style=\"padding:8px 0;border-bottom:1px solid #2a2a2a;color:#ddd;text-align:center;\">x")
//                         .append(item.getQuantity()).append("</td>")
//                         .append("<td style=\"padding:8px 0;border-bottom:1px solid #2a2a2a;color:#ddd;text-align:right;\">")
//                         .append(money(item.getPrice() * item.getQuantity())).append("</td>")
//                         .append("</tr>");
//             }
//         }

//         // Discount line (only when a promo was applied)
//         if (order.getDiscountAmount() != null && order.getDiscountAmount() > 0) {
//             rows.append("<tr><td colspan=\"2\" style=\"padding:8px 0;color:#10b981;\">Discount")
//                     .append(order.getPromoCode() != null ? " (" + esc(order.getPromoCode()) + ")" : "")
//                     .append("</td><td style=\"padding:8px 0;color:#10b981;text-align:right;\">-")
//                     .append(money(order.getDiscountAmount())).append("</td></tr>");
//         }

//         // Tables instead of flexbox: far more reliable across email clients.
//         return "<div style=\"font-family:Arial,sans-serif;background:#0B0F1A;padding:32px;\">"
//                 + "<div style=\"max-width:520px;margin:0 auto;background:#161D2E;border-radius:16px;padding:32px;color:#F5F5F7;\">"
//                 + "<h1 style=\"font-size:20px;margin:0 0 8px;color:#F5F5F7;\">" + esc(headline) + "</h1>"
//                 + "<p style=\"color:#8B92A8;font-size:14px;line-height:1.6;margin:0 0 24px;\">" + esc(message) + "</p>"
//                 + "<table style=\"width:100%;border-collapse:collapse;font-size:14px;\">" + rows + "</table>"
//                 + "<table style=\"width:100%;margin-top:16px;border-top:1px solid #2a2a2a;\"><tr>"
//                 + "<td style=\"padding-top:16px;\"><strong style=\"color:#F5F5F7;\">Total</strong></td>"
//                 + "<td style=\"padding-top:16px;text-align:right;\"><strong style=\"color:#FF3D71;\">"
//                 + money(order.getTotalAmount()) + "</strong></td></tr></table>"
//                 + "<p style=\"color:#6E7793;font-size:12px;margin-top:32px;\">Shipping to: "
//                 + esc(order.getShippingAddress()) + "</p>"
//                 + "</div></div>";
//     }

//     // Product names and addresses are user/admin supplied: never put them in HTML raw.
//     private String esc(String s) {
//         return s == null ? "" : HtmlUtils.htmlEscape(s);
//     }

//     private String money(double amount) {
//         return "₹" + String.format(Locale.ROOT, "%.2f", amount);
//     }

//     private String capitalize(String s) {
//         if (s == null || s.isEmpty()) return s;
//         return s.substring(0, 1).toUpperCase(Locale.ROOT) + s.substring(1);
//     }
// }

package com.example.MyProject.Services;

import com.example.MyProject.Enum.PaymentStatus;
import com.example.MyProject.Models.Order;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.util.Locale;

/**
 * Sends order emails. Fails soft: a mail problem is logged, never thrown.
 * Called from OrderEmailListener AFTER the order transaction commits, on a
 * background thread. Respects app.mail.enabled (default false).
 *
 * CHANGED: the confirmation wording now depends on how the order is paid (COD
 * vs card), and
 * every order email shows a "Payment" line, so the customer always knows what
 * is expected.
 */
@Service
@RequiredArgsConstructor
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    @Value("${app.mail.from-name:ShopSphere}")
    private String fromName;

    @Value("${spring.mail.username:}")
    private String fromAddress;

    public void sendOrderConfirmation(Order order) {
        String subject = "Order Confirmed - #" + order.getOrderId();

        // CHANGED: a COD customer is told to keep the cash ready; a card customer is
        // told the
        // payment was received (the card confirmation is only ever sent after payment
        // succeeds).
        String message = isCod(order)
                ? "We've received your order and it's now being processed. Please keep "
                        + money(order.getTotalAmount()) + " ready to pay when it arrives. Here's a summary:"
                : "We've received your payment and your order is now being processed. Here's a summary:";

        send(order.getUser().getEmail(), subject,
                buildOrderEmail(order, "Thanks for your order!", message));
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
        send(order.getUser().getEmail(), subject, buildOrderEmail(order, headline, message));
    }

    public void sendOrderCancelled(Order order) {
        String subject = "Order #" + order.getOrderId() + " - Cancelled";

        // Only promise a refund if money was actually taken.
        String message = (order.getPaymentStatus() == PaymentStatus.REFUND_PENDING)
                ? "Order #" + order.getOrderId() + " has been cancelled. Your payment will be refunded to your "
                        + "original payment method; once processed it can take a few business days to appear."
                : "Order #" + order.getOrderId() + " has been cancelled. No payment was taken for this order.";

        send(order.getUser().getEmail(), subject,
                buildOrderEmail(order, "Your order has been cancelled", message));
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
            log.warn("Failed to send email '{}' to {}: {}", subject, to, e.getMessage());
        }
    }

    private String buildOrderEmail(Order order, String headline, String message) {
        StringBuilder rows = new StringBuilder();
        if (order.getOrderItems() != null) {
            for (var item : order.getOrderItems()) {
                String name = item.getProduct() != null ? item.getProduct().getName() : "Item";
                rows.append("<tr>")
                        .append("<td style=\"padding:8px 0;border-bottom:1px solid #2a2a2a;color:#ddd;\">")
                        .append(esc(name));
                if (item.getVariantLabel() != null && !item.getVariantLabel().isBlank()) {
                    rows.append(" <span style=\"color:#888;\">(").append(esc(item.getVariantLabel()))
                            .append(")</span>");
                }
                rows.append("</td>")
                        .append("<td style=\"padding:8px 0;border-bottom:1px solid #2a2a2a;color:#ddd;text-align:center;\">x")
                        .append(item.getQuantity()).append("</td>")
                        .append("<td style=\"padding:8px 0;border-bottom:1px solid #2a2a2a;color:#ddd;text-align:right;\">")
                        .append(money(item.getPrice() * item.getQuantity())).append("</td>")
                        .append("</tr>");
            }
        }

        // Discount line (only when a promo was applied)
        if (order.getDiscountAmount() != null && order.getDiscountAmount() > 0) {
            rows.append("<tr><td colspan=\"2\" style=\"padding:8px 0;color:#10b981;\">Discount")
                    .append(order.getPromoCode() != null ? " (" + esc(order.getPromoCode()) + ")" : "")
                    .append("</td><td style=\"padding:8px 0;color:#10b981;text-align:right;\">-")
                    .append(money(order.getDiscountAmount())).append("</td></tr>");
        }

        // Tables instead of flexbox: far more reliable across email clients.
        return "<div style=\"font-family:Arial,sans-serif;background:#0B0F1A;padding:32px;\">"
                + "<div style=\"max-width:520px;margin:0 auto;background:#161D2E;border-radius:16px;padding:32px;color:#F5F5F7;\">"
                + "<h1 style=\"font-size:20px;margin:0 0 8px;color:#F5F5F7;\">" + esc(headline) + "</h1>"
                + "<p style=\"color:#8B92A8;font-size:14px;line-height:1.6;margin:0 0 24px;\">" + esc(message) + "</p>"
                + "<table style=\"width:100%;border-collapse:collapse;font-size:14px;\">" + rows + "</table>"
                + "<table style=\"width:100%;margin-top:16px;border-top:1px solid #2a2a2a;\"><tr>"
                + "<td style=\"padding-top:16px;\"><strong style=\"color:#F5F5F7;\">Total</strong></td>"
                + "<td style=\"padding-top:16px;text-align:right;\"><strong style=\"color:#FF3D71;\">"
                + money(order.getTotalAmount()) + "</strong></td></tr></table>"
                // NEW: how this order is being paid
                + "<p style=\"color:#8B92A8;font-size:13px;margin:16px 0 0;\">Payment: "
                + esc(paymentLabel(order)) + "</p>"
                + "<p style=\"color:#6E7793;font-size:12px;margin-top:16px;\">Shipping to: "
                + esc(order.getShippingAddress()) + "</p>"
                + "</div></div>";
    }

    // NEW: plain-words payment line for the email
    private String paymentLabel(Order order) {
        PaymentStatus ps = order.getPaymentStatus();
        if (ps == PaymentStatus.REFUND_PENDING)
            return "Refund pending";
        if (isCod(order)) {
            return ps == PaymentStatus.PAID ? "Cash on delivery (paid)" : "Cash on delivery (pay when it arrives)";
        }
        return ps == PaymentStatus.PAID ? "Paid online" : "Awaiting payment";
    }

    private boolean isCod(Order order) {
        return "COD".equalsIgnoreCase(order.getPaymentMethod());
    }

    // Product names and addresses are user/admin supplied: never put them in HTML
    // raw.
    private String esc(String s) {
        return s == null ? "" : HtmlUtils.htmlEscape(s);
    }

    private String money(double amount) {
        return "₹" + String.format(Locale.ROOT, "%.2f", amount);
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty())
            return s;
        return s.substring(0, 1).toUpperCase(Locale.ROOT) + s.substring(1);
    }
}
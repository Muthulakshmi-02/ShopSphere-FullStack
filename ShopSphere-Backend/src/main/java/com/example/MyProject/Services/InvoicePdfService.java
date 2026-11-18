package com.example.MyProject.Services;

import com.example.MyProject.Models.Order;
import com.example.MyProject.Models.OrderItem;
import com.example.MyProject.Models.User;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

@Service
public class InvoicePdfService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    private static final Color PRIMARY_COLOR = new Color(30, 41, 59);     // #1E293B
    private static final Color ACCENT_COLOR = new Color(99, 102, 241);     // #6366F1
    private static final Color HEADER_BG = new Color(241, 245, 249);       // #F1F5F9
    private static final Color BORDER_COLOR = new Color(226, 232, 240);    // #E2E8F0
    private static final Color MUTED_TEXT = new Color(100, 116, 139);      // #64748B

    public byte[] generateInvoice(Order order) {
        Document document = new Document(PageSize.A4, 36, 36, 40, 40);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // 1. Header (Brand + Invoice title)
            PdfPTable headerTable = new PdfPTable(2);
            headerTable.setWidthPercentage(100);
            headerTable.setWidths(new float[]{60, 40});

            PdfPCell brandCell = new PdfPCell();
            brandCell.setBorder(Rectangle.NO_BORDER);
            Paragraph brandName = new Paragraph("SHOPSPHERE", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, ACCENT_COLOR));
            Paragraph brandTagline = new Paragraph("Official Store & Order Receipt", FontFactory.getFont(FontFactory.HELVETICA, 10, MUTED_TEXT));
            brandCell.addElement(brandName);
            brandCell.addElement(brandTagline);
            headerTable.addCell(brandCell);

            PdfPCell invoiceMetaCell = new PdfPCell();
            invoiceMetaCell.setBorder(Rectangle.NO_BORDER);
            invoiceMetaCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            Paragraph invoiceTitle = new Paragraph("TAX INVOICE", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, PRIMARY_COLOR));
            invoiceTitle.setAlignment(Element.ALIGN_RIGHT);
            Paragraph invoiceNo = new Paragraph("Invoice #: INV-" + order.getOrderId(), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, PRIMARY_COLOR));
            invoiceNo.setAlignment(Element.ALIGN_RIGHT);
            Paragraph invoiceDate = new Paragraph("Date: " + order.getOrderDate().format(DATE_FORMATTER), FontFactory.getFont(FontFactory.HELVETICA, 10, MUTED_TEXT));
            invoiceDate.setAlignment(Element.ALIGN_RIGHT);
            invoiceMetaCell.addElement(invoiceTitle);
            invoiceMetaCell.addElement(invoiceNo);
            invoiceMetaCell.addElement(invoiceDate);
            headerTable.addCell(invoiceMetaCell);

            document.add(headerTable);
            document.add(new Paragraph(" "));

            // 2. Info Block (Customer & Order Info)
            PdfPTable infoTable = new PdfPTable(2);
            infoTable.setWidthPercentage(100);
            infoTable.setWidths(new float[]{55, 45});

            // Bill To
            PdfPCell billToCell = new PdfPCell();
            billToCell.setBorder(Rectangle.NO_BORDER);
            Paragraph billTitle = new Paragraph("BILLED & SHIPPED TO:", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, MUTED_TEXT));
            User u = order.getUser();
            String customerName = u != null ? u.getUserName() : "Customer";
            String customerEmail = u != null ? u.getEmail() : "";
            Paragraph nameP = new Paragraph(customerName, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, PRIMARY_COLOR));
            Paragraph addrP = new Paragraph(order.getShippingAddress() + "\n" + order.getCity() + ", " + order.getState() + " - " + order.getZipCode(), FontFactory.getFont(FontFactory.HELVETICA, 10, PRIMARY_COLOR));
            Paragraph contactP = new Paragraph("Phone: " + order.getPhoneNumber() + (customerEmail.isBlank() ? "" : "\nEmail: " + customerEmail), FontFactory.getFont(FontFactory.HELVETICA, 10, MUTED_TEXT));
            billToCell.addElement(billTitle);
            billToCell.addElement(nameP);
            billToCell.addElement(addrP);
            billToCell.addElement(contactP);
            infoTable.addCell(billToCell);

            // Order Status & Payment Meta
            PdfPCell orderMetaCell = new PdfPCell();
            orderMetaCell.setBorder(Rectangle.NO_BORDER);
            orderMetaCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            Paragraph orderTitle = new Paragraph("PAYMENT & STATUS:", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, MUTED_TEXT));
            orderTitle.setAlignment(Element.ALIGN_RIGHT);
            Paragraph statusP = new Paragraph("Order Status: " + order.getOrderStatus(), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, PRIMARY_COLOR));
            statusP.setAlignment(Element.ALIGN_RIGHT);
            Paragraph paymentMethodP = new Paragraph("Payment Method: " + order.getPaymentMethod(), FontFactory.getFont(FontFactory.HELVETICA, 10, PRIMARY_COLOR));
            paymentMethodP.setAlignment(Element.ALIGN_RIGHT);
            Color payColor = order.getPaymentStatus().name().equals("PAID") ? new Color(22, 163, 74) : ACCENT_COLOR;
            Paragraph paymentStatusP = new Paragraph("Payment Status: " + order.getPaymentStatus(), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, payColor));
            paymentStatusP.setAlignment(Element.ALIGN_RIGHT);
            orderMetaCell.addElement(orderTitle);
            orderMetaCell.addElement(statusP);
            orderMetaCell.addElement(paymentMethodP);
            orderMetaCell.addElement(paymentStatusP);
            infoTable.addCell(orderMetaCell);

            document.add(infoTable);
            document.add(new Paragraph(" "));

            // 3. Items Table
            PdfPTable itemsTable = new PdfPTable(5);
            itemsTable.setWidthPercentage(100);
            itemsTable.setWidths(new float[]{8, 46, 16, 12, 18});

            String[] headers = {"#", "Item Description", "Variant", "Qty", "Amount (INR)"};
            for (String h : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(h, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, PRIMARY_COLOR)));
                cell.setBackgroundColor(HEADER_BG);
                cell.setBorderColor(BORDER_COLOR);
                cell.setPadding(8);
                if (h.equals("Qty") || h.equals("Amount (INR)")) {
                    cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                }
                itemsTable.addCell(cell);
            }

            int index = 1;
            double itemsSubtotal = 0.0;
            if (order.getOrderItems() != null) {
                for (OrderItem item : order.getOrderItems()) {
                    String pName = item.getProduct() != null ? item.getProduct().getName() : "Product";
                    String vLabel = item.getVariantLabel() != null && !item.getVariantLabel().isBlank() ? item.getVariantLabel() : "-";
                    int qty = item.getQuantity() != null ? item.getQuantity() : 1;
                    double price = item.getPrice() != null ? item.getPrice() : 0.0;
                    double lineTotal = qty * price;
                    itemsSubtotal += lineTotal;

                    itemsTable.addCell(createCell(String.valueOf(index++), Element.ALIGN_CENTER, false));
                    itemsTable.addCell(createCell(pName, Element.ALIGN_LEFT, false));
                    itemsTable.addCell(createCell(vLabel, Element.ALIGN_LEFT, false));
                    itemsTable.addCell(createCell(String.valueOf(qty), Element.ALIGN_RIGHT, false));
                    itemsTable.addCell(createCell(String.format("%.2f", lineTotal), Element.ALIGN_RIGHT, false));
                }
            }

            document.add(itemsTable);

            // 4. Summary / Total Block
            PdfPTable summaryTable = new PdfPTable(2);
            summaryTable.setWidthPercentage(45);
            summaryTable.setHorizontalAlignment(Element.ALIGN_RIGHT);
            summaryTable.setWidths(new float[]{55, 45});

            addSummaryRow(summaryTable, "Subtotal:", String.format("%.2f", itemsSubtotal), false);
            if (order.getDiscountAmount() != null && order.getDiscountAmount() > 0) {
                String promo = order.getPromoCode() != null ? "Discount (" + order.getPromoCode() + "):" : "Discount:";
                addSummaryRow(summaryTable, promo, String.format("-%.2f", order.getDiscountAmount()), false);
            }
            addSummaryRow(summaryTable, "Total Amount:", String.format("INR %.2f", order.getTotalAmount()), true);

            document.add(summaryTable);
            document.add(new Paragraph(" "));

            // 5. Footer Notice
            Paragraph footerNote = new Paragraph(
                    "This is a computer-generated tax invoice and requires no physical signature.\n" +
                    "Thank you for your purchase with ShopSphere!",
                    FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 9, MUTED_TEXT)
            );
            footerNote.setAlignment(Element.ALIGN_CENTER);
            document.add(footerNote);

            document.close();
        } catch (DocumentException e) {
            throw new RuntimeException("Error generating invoice PDF", e);
        }

        return out.toByteArray();
    }

    private PdfPCell createCell(String text, int alignment, boolean bold) {
        Font font = bold
                ? FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, PRIMARY_COLOR)
                : FontFactory.getFont(FontFactory.HELVETICA, 9, PRIMARY_COLOR);
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(alignment);
        cell.setPadding(6);
        cell.setBorderColor(BORDER_COLOR);
        return cell;
    }

    private void addSummaryRow(PdfPTable table, String label, String value, boolean isTotal) {
        Font labelFont = isTotal
                ? FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, PRIMARY_COLOR)
                : FontFactory.getFont(FontFactory.HELVETICA, 10, MUTED_TEXT);
        Font valueFont = isTotal
                ? FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, ACCENT_COLOR)
                : FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, PRIMARY_COLOR);

        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setPadding(4);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, valueFont));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        valueCell.setPadding(4);
        table.addCell(valueCell);
    }
}

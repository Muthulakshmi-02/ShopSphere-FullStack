package com.example.MyProject.Services;

public record OrderEmailEvent(Long orderId, Type type) {
    // REFUNDED is new: sent after an admin refunds a cancelled, paid order.
    public enum Type { CONFIRMATION, STATUS_UPDATE, CANCELLED, REFUNDED }
}
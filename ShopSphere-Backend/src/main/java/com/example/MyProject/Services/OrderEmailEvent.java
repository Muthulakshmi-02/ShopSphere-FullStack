package com.example.MyProject.Services;

public record OrderEmailEvent(Long orderId, Type type) {
    public enum Type { CONFIRMATION, STATUS_UPDATE, CANCELLED }
}
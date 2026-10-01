package com.example.MyProject.Exception;
public class CartBusinessException extends RuntimeException {
    public CartBusinessException(String message) {
        super(message);
    }
}
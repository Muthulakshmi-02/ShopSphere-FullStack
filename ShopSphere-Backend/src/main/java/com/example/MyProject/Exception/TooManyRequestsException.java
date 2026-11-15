package com.example.MyProject.Exception;

/** Thrown when someone has tried too often (wrong passwords, reset requests...). Becomes HTTP 429. */
public class TooManyRequestsException extends RuntimeException {
    public TooManyRequestsException(String message) {
        super(message);
    }
}
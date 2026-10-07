package com.example.MyProject.Configuration;

import com.example.MyProject.Exception.CartBusinessException;
import com.example.MyProject.Exception.ResourceNotFoundException;
import com.example.MyProject.User.Dto.ApiResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.stream.Collectors;

/**
 * Turns every exception into the same ApiResponse JSON the frontend reads
 * (err.error?.message).
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleNotFound(ResourceNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(CartBusinessException.class)
    public ResponseEntity<ApiResponse<Object>> handleCartBusiness(CartBusinessException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    // State conflicts (e.g. paying for an already-paid order) -> 409
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Object>> handleIllegalState(IllegalStateException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    // NEW: PaymentService throws IllegalArgumentException with user-friendly messages
    // ("Order not found...", "Could not start payment right now..."). These used to fall
    // through to the catch-all and become a generic 500.
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Object>> handleIllegalArgument(IllegalArgumentException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    // NEW: unique-constraint races (two simultaneous registrations, deleting something
    // still referenced...) -> 409 instead of a raw 500.
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Object>> handleDataIntegrity(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
        return build(HttpStatus.CONFLICT,
                "This action conflicts with existing data (for example a duplicate value, or an item that is still in use).");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Object>> handleAccessDenied(AccessDeniedException ex) {
        return build(HttpStatus.FORBIDDEN, "You are not authorized to perform this action.");
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Object>> handleBadCredentials(BadCredentialsException ex) {
        return build(HttpStatus.UNAUTHORIZED, "Invalid email or password.");
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        return buildRaw(HttpStatus.BAD_REQUEST, message.isBlank() ? "Validation failed." : message);
    }

    // Only the human-readable messages, not "register.dto.email: ..." property paths.
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Object>> handleConstraintViolation(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("; "));
        return build(HttpStatus.BAD_REQUEST, message.isBlank() ? "Validation failed." : message);
    }

    @Override
    protected ResponseEntity<Object> handleMaxUploadSizeExceededException(
            org.springframework.web.multipart.MaxUploadSizeExceededException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return buildRaw(HttpStatus.BAD_REQUEST, "Image is too large - max size is 5MB.");
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return buildRaw(HttpStatus.BAD_REQUEST, "The request body is malformed or contains an invalid value.");
    }

    // NEW: everything ResponseEntityExceptionHandler handles itself (405 wrong method, 415,
    // missing parameter, bad path variable such as /products/abc, unknown URL...) used to
    // return Spring's ProblemDetail, which has "detail" but no "message", so the frontend
    // lost the text. This puts all of them into the ApiResponse shape too.
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        String message = (body instanceof ProblemDetail pd && pd.getDetail() != null)
                ? pd.getDetail()
                : "Request failed.";
        if (statusCode.is5xxServerError()) {
            log.error("Server error", ex);
            message = "Something went wrong. Please try again.";
        }
        ApiResponse<Object> payload = ApiResponse.builder().success(false).data(null).message(message).build();
        return ResponseEntity.status(statusCode).headers(headers).body(payload);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong. Please try again.");
    }

    private ResponseEntity<ApiResponse<Object>> build(HttpStatus status, String message) {
        ApiResponse<Object> body = ApiResponse.builder().success(false).data(null).message(message).build();
        return ResponseEntity.status(status).body(body);
    }

    // Same body, typed ResponseEntity<Object> for the overridden superclass hooks.
    private ResponseEntity<Object> buildRaw(HttpStatus status, String message) {
        ApiResponse<Object> body = ApiResponse.builder().success(false).data(null).message(message).build();
        return ResponseEntity.status(status).body(body);
    }
}
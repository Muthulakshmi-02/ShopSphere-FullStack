package com.example.MyProject.Configuration;

import com.example.MyProject.Exception.CartBusinessException;
import com.example.MyProject.Exception.ResourceNotFoundException;
import com.example.MyProject.User.Dto.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.stream.Collectors;

/**
 * Central place that turns every exception thrown by a controller/service
 * into the same {@link ApiResponse} JSON shape the frontend already expects
 * (it reads {@code err.error?.message} everywhere). Without this, Spring
 * Boot's default error handler returns a generic body with no "message"
 * field (message inclusion is disabled by default for security), so every
 * specific, helpful exception message in the app - "out of stock", "order
 * not found", "cart is empty", etc. - was silently being replaced by the
 * frontend's generic fallback text before this existed.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // --- Domain "not found" errors -> 404 ---
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleNotFound(ResourceNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    // --- Domain business-rule violations (out of stock, duplicate item, etc.) -> 400 ---
    @ExceptionHandler(CartBusinessException.class)
    public ResponseEntity<ApiResponse<Object>> handleCartBusiness(CartBusinessException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    // --- State conflicts (e.g. paying for an already-paid order) -> 409 ---
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Object>> handleIllegalState(IllegalStateException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    // --- Authorization failures (e.g. IDOR checks) -> 403 ---
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Object>> handleAccessDenied(AccessDeniedException ex) {
        return build(HttpStatus.FORBIDDEN, "You are not authorized to perform this action.");
    }

    // --- Bad credentials on login -> 401 with a clean message instead of a stack trace ---
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Object>> handleBadCredentials(BadCredentialsException ex) {
        return build(HttpStatus.UNAUTHORIZED, "Invalid email or password.");
    }

    // --- @Valid bean validation failures on request DTOs -> 400 with field-level messages ---
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        return buildRaw(HttpStatus.BAD_REQUEST, message.isBlank() ? "Validation failed." : message);
    }

    // --- @Validated path/query param validation failures -> 400 ---
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Object>> handleConstraintViolation(ConstraintViolationException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    // --- File too large during upload (e.g. product image over the configured limit) -> 400 ---
    // This overrides the protected hook ResponseEntityExceptionHandler itself
    // calls internally for this exception (confirmed from Spring's source:
    // handleException() delegates to handleMaxUploadSizeExceededException()).
    // A plain @ExceptionHandler(MaxUploadSizeExceededException.class) here
    // would register a SECOND, competing mapping for the same exception
    // type and fail app startup with "Ambiguous @ExceptionHandler method
    // mapped" - overriding the existing hook is the correct fix, same
    // pattern as handleMethodArgumentNotValid/handleHttpMessageNotReadable
    // below.
    @Override
    protected ResponseEntity<Object> handleMaxUploadSizeExceededException(
            org.springframework.web.multipart.MaxUploadSizeExceededException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return buildRaw(HttpStatus.BAD_REQUEST, "Image is too large - max size is 5MB.");
    }

    // --- Malformed JSON body / wrong enum value etc. -> 400 instead of a raw 500 ---
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return buildRaw(HttpStatus.BAD_REQUEST, "The request body is malformed or contains an invalid value.");
    }

    // --- Anything else: don't leak internals, but do log server-side for debugging ---
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong. Please try again.");
    }

    private ResponseEntity<ApiResponse<Object>> build(HttpStatus status, String message) {
        ApiResponse<Object> body = ApiResponse.builder()
                .success(false)
                .data(null)
                .message(message)
                .build();
        return ResponseEntity.status(status).body(body);
    }

    // Same body shape as build(), but typed ResponseEntity<Object> instead
    // of ResponseEntity<ApiResponse<Object>> - required specifically for
    // handleMethodArgumentNotValid/handleHttpMessageNotReadable, since
    // they override methods on ResponseEntityExceptionHandler that are
    // declared to return ResponseEntity<Object>. Java generics are
    // invariant, so ResponseEntity<ApiResponse<Object>> cannot be returned
    // where ResponseEntity<Object> is expected, even though ApiResponse<Object>
    // IS an Object - that was the exact compile error being fixed here.
    private ResponseEntity<Object> buildRaw(HttpStatus status, String message) {
        ApiResponse<Object> body = ApiResponse.builder()
                .success(false)
                .data(null)
                .message(message)
                .build();
        return ResponseEntity.status(status).body(body);
    }
}

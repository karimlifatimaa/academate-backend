package com.example.academatebackend.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiError> handleApiException(ApiException ex, HttpServletRequest request) {
        log.warn("ApiException [{}]: {}", ex.getStatus(), ex.getMessage());
        return ResponseEntity.status(ex.getStatus()).body(ApiError.builder()
                .status(ex.getStatus().value())
                .title(ex.getTitle())
                .detail(ex.getMessage())
                .traceId(getTraceId(request))
                .timestamp(Instant.now())
                .build());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidationException(MethodArgumentNotValidException ex,
                                                               HttpServletRequest request) {
        BindingResult bindingResult = ex.getBindingResult();
        List<ApiError.FieldError> fieldErrors = bindingResult.getFieldErrors().stream()
                .map(fe -> ApiError.FieldError.builder()
                        .field(fe.getField())
                        .message(fe.getDefaultMessage())
                        .build())
                .toList();

        return ResponseEntity.badRequest().body(ApiError.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .title("Validation Failed")
                .detail("One or more fields are invalid")
                .traceId(getTraceId(request))
                .timestamp(Instant.now())
                .errors(fieldErrors)
                .build());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiError.builder()
                .status(HttpStatus.FORBIDDEN.value())
                .title("Forbidden")
                .detail("You do not have permission to access this resource")
                .traceId(getTraceId(request))
                .timestamp(Instant.now())
                .build());
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuthentication(AuthenticationException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiError.builder()
                .status(HttpStatus.UNAUTHORIZED.value())
                .title("Unauthorized")
                .detail("Authentication required")
                .traceId(getTraceId(request))
                .timestamp(Instant.now())
                .build());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneral(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiError.builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .title("Internal Server Error")
                .detail("An unexpected error occurred")
                .traceId(getTraceId(request))
                .timestamp(Instant.now())
                .build());
    }

    private String getTraceId(HttpServletRequest request) {
        String traceId = request.getHeader("X-Request-Id");
        return traceId != null ? traceId : UUID.randomUUID().toString();
    }
}

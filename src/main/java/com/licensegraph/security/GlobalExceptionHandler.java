package com.licensegraph.security;

import com.licensegraph.common.error.ApiError;
import com.licensegraph.common.exception.ApiException;
import com.licensegraph.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Maps shared exceptions onto the Section 10 error body (E5). Controllers do not build error JSON.
 *
 * <p>Unexpected failures return 500 with {@code INTERNAL_ERROR} and a fixed message. The exception
 * text is logged and is not copied into the response. {@code INTERNAL_ERROR} is not a Section 10
 * business code and is not part of {@link ErrorCode}.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    static final String INTERNAL_ERROR = "INTERNAL_ERROR";

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiError> handleApiException(ApiException exception, HttpServletRequest request) {
        return response(ApiErrorFactory.from(exception, request.getRequestURI()));
    }

    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            ConstraintViolationException.class,
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<ApiError> handleValidation(Exception exception, HttpServletRequest request) {
        ApiError body = ApiErrorFactory.of(
                ErrorCode.VALIDATION_ERROR.status(),
                ErrorCode.VALIDATION_ERROR.name(),
                validationMessage(exception),
                request.getRequestURI(),
                null);
        return response(body);
    }

    /**
     * Last resort for failures that are not one of the shared API exceptions.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception exception, HttpServletRequest request) {
        log.error("Unhandled exception on {}", request.getRequestURI(), exception);
        ApiError body = ApiErrorFactory.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                INTERNAL_ERROR,
                "An unexpected error occurred",
                request.getRequestURI(),
                null);
        return response(body);
    }

    private static ResponseEntity<ApiError> response(ApiError body) {
        return ResponseEntity.status(body.status()).body(body);
    }

    private static String validationMessage(Exception exception) {
        if (exception instanceof MethodArgumentNotValidException invalid) {
            return invalid.getBindingResult().getFieldErrors().stream()
                    .map(error -> error.getField() + ": " + error.getDefaultMessage())
                    .collect(Collectors.joining("; "));
        }
        if (exception instanceof ConstraintViolationException violations) {
            return violations.getConstraintViolations().stream()
                    .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                    .collect(Collectors.joining("; "));
        }
        if (exception instanceof MethodArgumentTypeMismatchException mismatch) {
            return mismatch.getName() + " is invalid";
        }
        return "Request body is invalid";
    }
}

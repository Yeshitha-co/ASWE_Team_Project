package com.licensegraph.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.licensegraph.common.exception.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

/**
 * Writes the shared error JSON from a filter, where {@code @RestControllerAdvice}
 * does not run.
 */
@Component
public class ApiErrorResponseWriter {

    private final ObjectMapper objectMapper;

    public ApiErrorResponseWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Sets the status and body for an API failure. Does nothing if the response is already
     * committed.
     */
    public void write(
            HttpServletResponse response, HttpServletRequest request, ApiException exception)
            throws IOException {
        if (response.isCommitted()) {
            return;
        }
        response.setStatus(exception.status());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(
                response.getOutputStream(),
                ApiErrorFactory.from(exception, request.getRequestURI()));
    }
}

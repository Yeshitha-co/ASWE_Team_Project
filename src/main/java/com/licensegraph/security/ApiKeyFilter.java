package com.licensegraph.security;

import com.licensegraph.common.exception.UnauthorizedException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpMethod;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * AI-assisted: API-key authentication.
 *
 * <p>Requires {@code X-API-Key} on every route except {@code POST /api/clients} (E3).
 */
public class ApiKeyFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-API-Key";

    private static final String CLIENT_REGISTRATION_PATH = "/api/clients";

    private final ApiClientRepository clients;
    private final ApiErrorResponseWriter errors;

    public ApiKeyFilter(ApiClientRepository clients, ApiErrorResponseWriter errors) {
        this.clients = clients;
        this.errors = errors;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return HttpMethod.POST.matches(request.getMethod())
                && CLIENT_REGISTRATION_PATH.equals(pathWithinApplication(request));
    }

    /**
     * MockMvc leaves {@code servletPath} empty and puts the path in the request URI.
     * A deployed app may also prefix a context path.
     */
    private static String pathWithinApplication(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isEmpty() && uri.startsWith(contextPath)) {
            return uri.substring(contextPath.length());
        }
        return uri;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String rawKey = request.getHeader(HEADER);
        if (rawKey == null || rawKey.isBlank()) {
            errors.write(response, request, UnauthorizedException.missingApiKey());
            return;
        }
        String clientId = clients.findByKeyHash(ApiKeyHasher.sha256Hex(rawKey))
                .map(ApiClient::getClientId)
                .orElse(null);
        if (clientId == null) {
            errors.write(response, request, UnauthorizedException.invalidApiKey());
            return;
        }
        ClientIdHolder.set(clientId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            ClientIdHolder.clear();
        }
    }
}

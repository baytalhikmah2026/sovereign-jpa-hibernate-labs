package com.baytalhikmah.sovereign.tenant;

import java.io.IOException;
import java.util.Optional;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Every request to /api/ must carry a valid tenant token ("Authorization: Bearer ..."). The tenant from the
 * token is set for exactly this request; requests without a valid token are refused with 401.
 */
@Component
@Profile("tenancy")
public class TenantTokenFilter extends OncePerRequestFilter {

    private static final String BEARER = "Bearer ";

    private final TenantTokens tenantTokens;

    public TenantTokenFilter(TenantTokens tenantTokens) {
        this.tenantTokens = tenantTokens;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        Optional<String> tenant = header != null && header.startsWith(BEARER)
            ? tenantTokens.tenantOf(header.substring(BEARER.length()))
            : Optional.empty();
        if (tenant.isEmpty()) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "A valid tenant token is required");
            return;
        }
        try (TenantContext.Scope scope = TenantContext.enter(tenant.get())) {
            chain.doFilter(request, response);
        }
    }

}

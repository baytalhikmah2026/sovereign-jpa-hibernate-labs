package com.baytalhikmah.sovereign.web;

import java.io.IOException;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.baytalhikmah.sovereign.audit.RequestOrigin;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Remembers the caller's network address for the duration of each HTTP request (read by the audit trail). */
@Component
public class RequestOriginFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try (RequestOrigin.Scope scope = RequestOrigin.enter(request.getRemoteAddr())) {
            chain.doFilter(request, response);
        }
    }

}

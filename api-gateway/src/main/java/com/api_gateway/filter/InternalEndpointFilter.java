package com.api_gateway.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.regex.Pattern;

/*
    SAGA endpoint'leri (deduct / credit / refund) sadece servisler arasi cagrilir.
    Gateway uzerinden disariya acilirsa herkes bakiye degistirebilir -> 403
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class InternalEndpointFilter extends OncePerRequestFilter {

    private static final Pattern INTERNAL_ENDPOINTS =
            Pattern.compile("^/api/v1/accounts/[^/]+/(deduct|credit|refund)/?$", Pattern.CASE_INSENSITIVE);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        // getRequestURI() ham haliyle ";param", "%2e" gibi hilelerle atlatilabilir, normalize edilmis yolu kullan
        String path = request.getServletPath() + (request.getPathInfo() != null ? request.getPathInfo() : "");
        if (INTERNAL_ENDPOINTS.matcher(path).matches()) {
            log.warn("Blocked external call to internal endpoint : {} {}", request.getMethod(), request.getRequestURI());
            response.sendError(HttpStatus.FORBIDDEN.value(), "Internal endpoint");
            return;
        }
        filterChain.doFilter(request, response);
    }
}

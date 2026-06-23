package com.moni.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class GatewayHeaderAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/api/v1/auth/")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/actuator");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        String gatewaySecret = request.getHeader("X-Gateway-Secret");
        String expectedSecret = System.getenv().getOrDefault("GATEWAY_SECRET", "local-secret");

        if (!expectedSecret.equals(gatewaySecret)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Direct access not allowed");
            return;
        }

        String userId = request.getHeader("X-User-Id");
        String userRole = request.getHeader("X-User-Role");

        if (userId != null) {
            List<SimpleGrantedAuthority> authorities = (userRole != null)
                    ? Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + userRole))
                    : Collections.emptyList();
            CustomUserDetails userDetails = new CustomUserDetails(userId, userRole, authorities);
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userDetails, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }
}

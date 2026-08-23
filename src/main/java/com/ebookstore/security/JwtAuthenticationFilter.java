package com.ebookstore.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Reads the {@code Authorization: Bearer <token>} header, validates the JWT,
 * and sets the authenticated principal in the {@link SecurityContextHolder}.
 *
 * <p>Any token parse or validation error is caught silently and the request
 * continues unauthenticated — Spring Security then produces a {@code 401}
 * response via {@link AuthEntryPoint}.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsServiceImpl userDetailsService;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider,
                                   UserDetailsServiceImpl userDetailsService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String token = extractToken(request);
            if (StringUtils.hasText(token)) {
                String email = jwtTokenProvider.extractEmail(token);
                if (StringUtils.hasText(email)
                        && SecurityContextHolder.getContext().getAuthentication() == null) {

                    AuthenticatedUser userDetails =
                            (AuthenticatedUser) userDetailsService.loadUserByUsername(email);

                    if (jwtTokenProvider.isTokenValid(token, userDetails)) {
                        UsernamePasswordAuthenticationToken auth =
                                new UsernamePasswordAuthenticationToken(
                                        userDetails,
                                        null,
                                        userDetails.getAuthorities());
                        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    }
                }
            }
        } catch (Exception e) {
            // Do not propagate — leave request unauthenticated; AuthEntryPoint returns 401
            log.debug("JWT authentication failed for {}: {}", request.getRequestURI(), e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    /** Returns the raw token string or {@code null} if the header is absent/malformed. */
    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}

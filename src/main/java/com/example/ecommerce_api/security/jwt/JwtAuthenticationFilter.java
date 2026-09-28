package com.example.ecommerce_api.security.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;


@RequiredArgsConstructor
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final UserDetailsService userDetailsService;

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            String jwt = parseJwt(request);

            logger.info("Request: {} {}", request.getMethod(), request.getRequestURI());
            logger.info("JWT present: {}", jwt != null);

            if (jwt != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                jwtUtils.extractValidSubject(jwt).ifPresentOrElse(
                        username -> {

                            logger.info("JWT subject: {}", username);

                            UserDetails userDetails =
                                    userDetailsService.loadUserByUsername(username);

                            logger.info(
                                    "Authorities: {}",
                                    userDetails.getAuthorities()
                            );

                            UsernamePasswordAuthenticationToken authenticationToken =
                                    new UsernamePasswordAuthenticationToken(
                                            userDetails,
                                            null,
                                            userDetails.getAuthorities()
                                    );

                            authenticationToken.setDetails(
                                    new WebAuthenticationDetailsSource()
                                            .buildDetails(request)
                            );

                            SecurityContextHolder
                                    .getContext()
                                    .setAuthentication(authenticationToken);

                            logger.info(
                                    "Authenticated: {}",
                                    SecurityContextHolder
                                            .getContext()
                                            .getAuthentication()
                            );
                        },
                        () -> logger.warn("JWT is invalid or subject could not be extracted")
                );
            }

        } catch (Exception e) {
            logger.error("Cannot set user authentication", e);
        }

        filterChain.doFilter(request, response);
    }

    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");
        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }

        return null;
    }
}

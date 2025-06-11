package com.nextgenbank.backend.filter;

import com.nextgenbank.backend.security.JwtProvider;
import com.nextgenbank.backend.service.EmailUserDetailsService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final EmailUserDetailsService userDetailsService;

    public JwtFilter(JwtProvider jwtProvider, EmailUserDetailsService userDetailsService) {
        this.jwtProvider = jwtProvider;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/auth")
                || path.startsWith("/api/test")
                || path.equals("/v3/api-docs")
                || path.startsWith("/v3/api-docs/")
                || path.equals("/swagger-ui.html")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/swagger-resources")
                || path.startsWith("/webjars")
                || path.startsWith("/configuration")
                || path.startsWith("/favicon.ico")
                || path.startsWith("/h2-console");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7); // Remove "Bearer " prefix
            Claims claims = jwtProvider.extractAllClaims(token); //decodes the JWT and verifies its signature

            //Header, Payload (Claims), and Signature
            String email = claims.getSubject(); //Claims: Contains JWT payload (e.g., { "sub": "user@example.com", "role": "CUSTOMER" })
            String role = claims.get("role", String.class); // Extracts custom "role" claim

            //.getContext() returns the SecurityContext which holds the Authentication object
            //.getAuthentication() returns the current Authentication object
            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                //creates an Authentication object
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails, null,
                                Collections.singleton(new SimpleGrantedAuthority("ROLE_" + role))
                                // hte goal is to create a list with one item (like "ROLE_CUSTOMER")
                        );

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        // this proceeds to the next filter (or controller if no more filters exist)
        filterChain.doFilter(request, response);
    }
}

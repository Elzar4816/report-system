package org.example.reportsystem.security;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import io.jsonwebtoken.JwtException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

import org.example.reportsystem.service.AppUserDetailsService;
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtService jwt;
    private final AppUserDetailsService uds;

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        String token = null;

        // 1) Authorization: Bearer ...
        String h = req.getHeader(HttpHeaders.AUTHORIZATION);
        if (h != null && h.startsWith("Bearer ")) token = h.substring(7);

        // 2) Cookie: access=...
        if (token == null && req.getCookies() != null) {
            for (var c : req.getCookies()) {
                if ("access".equals(c.getName())) { token = c.getValue(); break; }
            }
        }

        if (token != null) {
            try {
                var sub = jwt.claims(token).getSubject();
                var user = uds.loadUserByUsername(sub);
                var auth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (JwtException ignored) {
                SecurityContextHolder.clearContext();
            }
        }

        chain.doFilter(req, res);
    }
}


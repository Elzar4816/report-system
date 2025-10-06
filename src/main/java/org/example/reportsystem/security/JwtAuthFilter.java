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

        String h = req.getHeader(HttpHeaders.AUTHORIZATION);
        if (h != null && h.startsWith("Bearer ")) token = h.substring(7);

        if (token == null && req.getCookies() != null) {
            for (var c : req.getCookies()) {
                if ("access".equals(c.getName())) { token = c.getValue(); break; }
            }
        }

        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                // username + authorities из токена
                var username = jwt.extractUsername(token);
                var authorities = jwt.extractRoles(token).stream()
                        .map(r -> "ROLE_" + r.toUpperCase())
                        .map(org.springframework.security.core.authority.SimpleGrantedAuthority::new)
                        .toList();

                //  флаги (enabled, locked), можешь слить с UserDetails:
                // var ud = uds.loadUserByUsername(username);
                // var merged = Stream.concat(ud.getAuthorities().stream(), authorities.stream()).distinct().toList();
                // var auth = new UsernamePasswordAuthenticationToken(ud, null, merged);

                var auth = new UsernamePasswordAuthenticationToken(username, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (io.jsonwebtoken.JwtException ignored) {
                SecurityContextHolder.clearContext();
            }
        }

        chain.doFilter(req, res);
    }
}
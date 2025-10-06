package org.example.reportsystem.security;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;

@Component
public class JwtService {
    @Value("${jwt.secret}") private String secret;
    @Value("${jwt.access.ttl-min}") private long accessTtlMin;
    @Value("${jwt.refresh.ttl-days}") private long refreshTtlDays;

    private SecretKey key() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }

    // access: sub + roles
    public String genAccess(String sub, Collection<String> roles) {
        return Jwts.builder()
                .subject(sub)
                .claim("roles", roles) // ["admin","user"]
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plus(Duration.ofMinutes(accessTtlMin))))
                .signWith(key(), Jwts.SIG.HS256)
                .compact();
    }

    // refresh: sub + jti
    public String genRefresh(String sub, String jti) {
        return Jwts.builder()
                .subject(sub)
                .id(jti)
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plus(Duration.ofDays(refreshTtlDays))))
                .signWith(key(), Jwts.SIG.HS256)
                .compact();
    }

    public Claims claims(String token) {
        return Jwts.parser()
                .verifyWith(key())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Instant expiresAt(String token) {
        return claims(token).getExpiration().toInstant();
    }

    public String extractUsername(String token) {
        return claims(token).getSubject();
    }

    @SuppressWarnings("unchecked")
    public java.util.List<String> extractRoles(String token) {
        Object v = claims(token).get("roles");
        if (v instanceof Collection<?> col) {
            return col.stream().map(String::valueOf).toList();
        }
        return java.util.List.of();
    }

    public boolean isValid(String token) {
        try {
            claims(token);
            return true;
        } catch (io.jsonwebtoken.JwtException e) {
            return false;
        }
    }
}
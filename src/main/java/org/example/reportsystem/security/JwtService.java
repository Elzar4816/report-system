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

import org.springframework.stereotype.Component;

@Component
public class JwtService {
    @Value("${jwt.secret}") private String secret;
    @Value("${jwt.access.ttl-min}") private long accessTtlMin;
    @Value("${jwt.refresh.ttl-days}") private long refreshTtlDays;

    private SecretKey key() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }

    public String genAccess(String sub, Collection<String> roles) {
        return Jwts.builder()
                .subject(sub).claim("roles", roles)
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plus(Duration.ofMinutes(accessTtlMin))))
                .signWith(key(), Jwts.SIG.HS256).compact();
    }

    public String genRefresh(String sub, String jti) {
        return Jwts.builder()
                .subject(sub).id(jti)
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plus(Duration.ofDays(refreshTtlDays))))
                .signWith(key(), Jwts.SIG.HS256).compact();
    }

    public Claims claims(String token) {
        return Jwts.parser().verifyWith(key()).build()
                .parseSignedClaims(token).getPayload();
    }
}


package org.example.reportsystem.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.example.reportsystem.model.Token;
import org.example.reportsystem.model.TokenType;
import org.example.reportsystem.repository.TokenRepository;
import org.example.reportsystem.repository.UserRepository;
import org.example.reportsystem.security.JwtService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final JwtService jwt;
    private final UserRepository users;
    private final TokenRepository tokens;

    public record RefreshResult(String access, String refresh, Instant accessExp, Instant refreshExp) {}

    /** Ротация refresh + выпуск нового access. */
    @Transactional
    public RefreshResult rotateRefresh(String refreshJwt, String userAgent, String ip) {
        Claims c;
        try { c = jwt.claims(refreshJwt); }
        catch (JwtException e) { throw new NoSuchElementException("invalid refresh"); }

        var jti = c.getId();
        var sub = c.getSubject();

        // 1) проверяем наличие активной записи
        tokens.findActiveRefresh(jti, Instant.now())
                .orElseThrow(() -> new NoSuchElementException("refresh not active"));

        // 2) ревокация старого
        tokens.revoke(jti);

        // 3) выпуск пары
        var user = users.findByUsername(sub).orElseThrow();
        var roles = List.of("ROLE_" + user.getRole().name());

        var newRefreshJti = UUID.randomUUID().toString();
        var access  = jwt.genAccess(sub, roles);
        var refresh = jwt.genRefresh(sub, newRefreshJti);

        // 4) сохраняем новый refresh
        tokens.save(Token.builder()
                .id(newRefreshJti)
                .user(user)
                .type(TokenType.REFRESH)
                .expiresAt(jwt.expiresAt(refresh))
                .revoked(false)
                .userAgent(userAgent)
                .ip(ip)
                .createdAt(Instant.now())
                .build());

        return new RefreshResult(access, refresh, jwt.expiresAt(access), jwt.expiresAt(refresh));
    }

    /** Ревокация конкретного refresh (logout). */
    @Transactional
    public void revokeRefresh(String refreshJwt) {
        try {
            var jti = jwt.claims(refreshJwt).getId();
            tokens.revoke(jti);
        } catch (JwtException ignored) { /* тихо игнорим битые */ }
    }
}

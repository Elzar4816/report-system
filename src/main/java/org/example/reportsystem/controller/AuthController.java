package org.example.reportsystem.controller;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.reportsystem.model.Role;
import org.example.reportsystem.model.User;
import org.example.reportsystem.repository.UserRepository;
import org.example.reportsystem.security.JwtService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthenticationManager authManager;
    private final JwtService jwt;
    private final UserRepository users;
    private final PasswordEncoder enc;
    private final Set<String> revoked = ConcurrentHashMap.newKeySet();

    // DEV-профиль: для http локально
    private static final boolean COOKIE_SECURE = false;     // prod: true
    private static final String COOKIE_SAMESITE = "Lax";    // prod: "Strict"

    public record LoginReq(String username, String password) {}
    public record RegisterReq(String username, String email, String password, String name) {}

    private ResponseCookie accessCookie(String token, Duration ttl) {
        return ResponseCookie.from("access", token)
                .httpOnly(true).secure(COOKIE_SECURE).sameSite(COOKIE_SAMESITE)
                .path("/")                      // доступна везде
                .maxAge(ttl)
                .build();
    }

    private ResponseCookie refreshCookie(String token, Duration ttl) {
        return ResponseCookie.from("refresh", token)
                .httpOnly(true).secure(COOKIE_SECURE).sameSite(COOKIE_SAMESITE)
                .path("/auth")                  // только для /auth/*
                .maxAge(ttl)
                .build();
    }

    private ResponseCookie clearCookie(String name) {
        String path = "refresh".equals(name) ? "/auth" : "/";
        return ResponseCookie.from(name, "")
                .httpOnly(true).secure(COOKIE_SECURE).sameSite(COOKIE_SAMESITE)
                .path(path).maxAge(0)
                .build();
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody @Valid RegisterReq r) {
        if (users.findByUsername(r.username()).isPresent()) return ResponseEntity.status(409).build();
        var u = new User();
        u.setUsername(r.username()); u.setEmail(r.email()); u.setName(r.name());
        u.setRole(Role.USER);
        u.setPassword(enc.encode(r.password()));
        users.save(u);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/login")
    public ResponseEntity<Void> login(@RequestBody @Valid LoginReq r, HttpServletResponse res) {
        authManager.authenticate(new UsernamePasswordAuthenticationToken(r.username(), r.password()));
        var u = users.findByUsername(r.username()).orElseThrow();
        var roles = List.of(u.getRole().name());

        var access  = jwt.genAccess(u.getUsername(), roles);
        var refresh = jwt.genRefresh(u.getUsername(), UUID.randomUUID().toString());

        res.addHeader(HttpHeaders.SET_COOKIE, accessCookie(access, Duration.ofMinutes(15)).toString());
        res.addHeader(HttpHeaders.SET_COOKIE, refreshCookie(refresh, Duration.ofDays(14)).toString());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/refresh")
    public ResponseEntity<Void> refresh(@CookieValue("refresh") String refresh, HttpServletResponse res) {
        try {
            var c = jwt.claims(refresh);
            if (revoked.contains(c.getId())) return ResponseEntity.status(401).build();

            var u = users.findByUsername(c.getSubject()).orElseThrow();
            var access = jwt.genAccess(u.getUsername(), List.of(u.getRole().name()));

            res.addHeader(HttpHeaders.SET_COOKIE, accessCookie(access, Duration.ofMinutes(15)).toString());
            return ResponseEntity.noContent().build();
        } catch (JwtException e) {
            return ResponseEntity.status(401).build();
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@CookieValue(name="refresh", required=false) String refresh,
                                       HttpServletResponse res) {
        if (refresh != null) {
            try { revoked.add(jwt.claims(refresh).getId()); } catch (JwtException ignored) {}
        }
        res.addHeader(HttpHeaders.SET_COOKIE, clearCookie("access").toString());
        res.addHeader(HttpHeaders.SET_COOKIE, clearCookie("refresh").toString());
        return ResponseEntity.noContent().build();
    }
}

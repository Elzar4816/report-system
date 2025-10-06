package org.example.reportsystem.controller;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.reportsystem.dto.ProfileDto;
import org.example.reportsystem.model.Role;
import org.example.reportsystem.model.User;
import org.example.reportsystem.repository.TokenRepository;
import org.example.reportsystem.repository.UserRepository;
import org.example.reportsystem.security.JwtService;
import org.example.reportsystem.model.Token;
import org.example.reportsystem.model.TokenType;
import org.example.reportsystem.service.AuthService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import org.springframework.security.core.userdetails.UserDetails;
import java.time.Duration;
import java.time.Instant;
import java.util.NoSuchElementException;

import java.util.UUID;


@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthenticationManager authManager;
    private final JwtService jwt;
    private final UserRepository users;
    private final TokenRepository tokenRepo;
    private final PasswordEncoder enc;
    private final AuthService authService;

    private static final boolean COOKIE_SECURE = false;     // prod: true
    private static final String COOKIE_SAMESITE = "Lax";    // prod: "Strict"

    public record LoginReq(String username, String password) {}
    public record RegisterReq(String username, String email, String password, String name) {}

    private ResponseCookie accessCookie(String token, Duration ttl) {
        return ResponseCookie.from("access", token)
                .httpOnly(true).secure(COOKIE_SECURE).sameSite(COOKIE_SAMESITE)
                .path("/")
                .maxAge(ttl)
                .build();
    }

    private ResponseCookie refreshCookie(String token, Duration ttl) {
        return ResponseCookie.from("refresh", token)
                .httpOnly(true).secure(COOKIE_SECURE).sameSite(COOKIE_SAMESITE)
                .path("/auth")
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
    public ResponseEntity<?> login(@Valid @RequestBody LoginReq dto,
                                   HttpServletRequest req,
                                   HttpServletResponse res) {
        var auth = authManager.authenticate(
                new UsernamePasswordAuthenticationToken(dto.username(), dto.password())
        );


        var principal = (UserDetails) auth.getPrincipal();

        var user = users.findByUsername(principal.getUsername()).orElseThrow();

        var roles = java.util.List.of(user.getRole().name().toLowerCase());

        var refreshJti = UUID.randomUUID().toString();

        var access  = jwt.genAccess(user.getUsername(), roles);
        var refresh = jwt.genRefresh(user.getUsername(), refreshJti);

        tokenRepo.save(Token.builder()
                .id(refreshJti)
                .user(user)
                .type(TokenType.REFRESH)
                .expiresAt(jwt.expiresAt(refresh))
                .revoked(false)
                .userAgent(req.getHeader("User-Agent"))
                .ip(req.getRemoteAddr())
                .createdAt(Instant.now())
                .build());

        var now = Instant.now();
        var accessTtl  = Duration.between(now, jwt.expiresAt(access));
        var refreshTtl = Duration.between(now, jwt.expiresAt(refresh));

        res.addHeader(HttpHeaders.SET_COOKIE, accessCookie(access, accessTtl).toString());
        res.addHeader(HttpHeaders.SET_COOKIE, refreshCookie(refresh, refreshTtl).toString());

        return ResponseEntity.ok().build();
    }

    @PostMapping("/refresh")
    public ResponseEntity<Void> refresh(@CookieValue(name="refresh", required=false) String refreshCookie,
                                        HttpServletRequest req,
                                        HttpServletResponse res) {
        if (refreshCookie == null) return ResponseEntity.status(401).build();
        try {
            var r = authService.rotateRefresh(
                    refreshCookie,
                    req.getHeader("User-Agent"),
                    req.getRemoteAddr()
            );

            var now = Instant.now();
            var accessTtl  = Duration.between(now, r.accessExp());
            var refreshTtl = Duration.between(now, r.refreshExp());

            res.addHeader(HttpHeaders.SET_COOKIE, accessCookie(r.access(), accessTtl).toString());
            res.addHeader(HttpHeaders.SET_COOKIE, refreshCookie(r.refresh(), refreshTtl).toString());
            return ResponseEntity.noContent().build();
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(401).build();
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@CookieValue(name="refresh", required=false) String refresh,
                                       HttpServletResponse res) {
        if (refresh != null) authService.revokeRefresh(refresh);
        res.addHeader(HttpHeaders.SET_COOKIE, clearCookie("access").toString());
        res.addHeader(HttpHeaders.SET_COOKIE, clearCookie("refresh").toString());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/profile")
    public ProfileDto profile(org.springframework.security.core.Authentication auth) {
        var u = users.findByUsername(auth.getName()).orElseThrow();
        var roles = auth.getAuthorities().stream()
                .map(org.springframework.security.core.GrantedAuthority::getAuthority) // ROLE_ADMIN
                .map(a -> a.startsWith("ROLE_") ? a.substring(5) : a)                  // ADMIN
                .map(String::toLowerCase)                                              // admin
                .toList();
        return new ProfileDto(u.getId(), u.getUsername(), u.getEmail(), roles);
    }

}

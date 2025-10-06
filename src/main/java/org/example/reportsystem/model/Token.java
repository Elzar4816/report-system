package org.example.reportsystem.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "tokens")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Token {

    @Id
    @Column(length = 36)
    private String id;              // jti

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TokenType type;         // ACCESS, REFRESH

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    @Builder.Default
    private boolean revoked = false;

    private String userAgent;
    private String ip;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}

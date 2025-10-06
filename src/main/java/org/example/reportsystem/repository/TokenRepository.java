package org.example.reportsystem.repository;

import org.example.reportsystem.model.Token;
import org.example.reportsystem.model.TokenType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface TokenRepository extends JpaRepository<Token, String> {

    @Query("""
      select t from Token t
      where t.id = :jti
        and t.type = org.example.reportsystem.model.TokenType.REFRESH
        and t.revoked = false
        and t.expiresAt > :now
    """)
    Optional<Token> findActiveRefresh(@Param("jti") String jti, @Param("now") Instant now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Token t set t.revoked = true where t.id = :jti")
    int revoke(@Param("jti") String jti);
}

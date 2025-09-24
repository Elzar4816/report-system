package org.example.reportsystem.repository;

import org.example.reportsystem.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    @Query("select u.email from User u where u.id in :ids")
    List<String> findEmailsByIds(@Param("ids") Collection<Long> ids);
}


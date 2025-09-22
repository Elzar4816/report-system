package org.example.reportsystem.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name="users", uniqueConstraints={
        @UniqueConstraint(columnNames={"username"}),
        @UniqueConstraint(columnNames={"email"})
})
@Data
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Enumerated(EnumType.STRING)
    private Role role;

    private String username;
    private String email;
    private String password;
}


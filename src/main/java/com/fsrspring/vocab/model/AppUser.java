package com.fsrspring.vocab.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "app_user")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column
    private String name;

    @Column
    private String avatarUrl;

    @Column(unique = true)
    private String googleId;

    public enum Role {
        USER, ADMIN
    }

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Role role = Role.USER;

    @Column
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column
    @Builder.Default
    private LocalDateTime lastLoginAt = LocalDateTime.now();
}
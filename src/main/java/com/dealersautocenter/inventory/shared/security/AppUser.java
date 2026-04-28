package com.dealersautocenter.inventory.shared.security;

import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Table(name = "app_users")
@Getter
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    /** Null for GLOBAL_ADMIN — not bound to any specific tenant. */
    @Column(name = "tenant_id")
    private String tenantId;

    @Column(nullable = false)
    private String role;
}

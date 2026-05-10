package com.molla.domain.auth;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "auth_codes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuthCode {

    @Id
    @Column(length = 36, columnDefinition = "CHAR(36)")
    private String id;

    @Column(name = "phone_number", nullable = false, length = 20)
    private String phoneNumber;

    @Column(nullable = false, length = 6)
    private String code;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "is_verified", nullable = false)
    private boolean isVerified;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public AuthCode(String phoneNumber, String code, LocalDateTime expiresAt) {
        this.id = UUID.randomUUID().toString();
        this.phoneNumber = phoneNumber;
        this.code = code;
        this.expiresAt = expiresAt;
        this.isVerified = false;
        this.createdAt = LocalDateTime.now();
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiresAt);
    }

    public void markVerified() {
        this.isVerified = true;
    }
}

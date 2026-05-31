package com.molla.domain.inquiry;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "inquiries")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Inquiry {

    @Id
    @Column(length = 36, columnDefinition = "CHAR(36)")
    private String id;

    @Column(length = 50)
    private String name;

    @Column(length = 100)
    private String email;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "is_read", nullable = false)
    private boolean read = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public static Inquiry create(String name, String email, String content) {
        Inquiry i = new Inquiry();
        i.id = UUID.randomUUID().toString();
        i.name = name;
        i.email = email;
        i.content = content;
        i.read = false;
        i.createdAt = LocalDateTime.now();
        return i;
    }

    public void markRead() {
        this.read = true;
    }
}

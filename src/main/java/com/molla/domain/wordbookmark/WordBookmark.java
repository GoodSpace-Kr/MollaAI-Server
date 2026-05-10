package com.molla.domain.wordbookmark;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "word_bookmarks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WordBookmark {

    @Id
    @Column(length = 36, columnDefinition = "CHAR(36)")
    private String id;

    @Column(name = "user_id", nullable = false, length = 36, columnDefinition = "CHAR(36)")
    private String userId;

    @Column(name = "session_id", length = 36, columnDefinition = "CHAR(36)")
    private String sessionId;           // nullable — 어느 통화에서 저장했는지

    @Column(nullable = false, length = 100)
    private String word;

    @Column(columnDefinition = "TEXT")
    private String definition;

    @Column(columnDefinition = "TEXT")
    private String example;

    @Column(name = "saved_at", nullable = false, updatable = false)
    private LocalDateTime savedAt;

    // ──────────────────────────────────────────────
    // 정적 팩토리
    // ──────────────────────────────────────────────

    public static WordBookmark create(
            String userId,
            String sessionId,
            String word,
            String definition,
            String example
    ) {
        WordBookmark bookmark = new WordBookmark();
        bookmark.id = UUID.randomUUID().toString();
        bookmark.userId = userId;
        bookmark.sessionId = sessionId;
        bookmark.word = word;
        bookmark.definition = definition;
        bookmark.example = example;
        bookmark.savedAt = LocalDateTime.now();
        return bookmark;
    }

    public boolean isOwnedBy(String userId) {
        return this.userId.equals(userId);
    }
}

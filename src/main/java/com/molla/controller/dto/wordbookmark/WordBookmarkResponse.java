package com.molla.controller.dto.wordbookmark;

import com.molla.domain.wordbookmark.WordBookmark;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "단어장 응답")
public record WordBookmarkResponse(

        @Schema(description = "단어장 항목 ID", example = "550e8400-e29b-41d4-a716-446655440000")
        String id,

        @Schema(description = "연결된 세션 ID (없으면 null)", example = "550e8400-e29b-41d4-a716-446655440000")
        String sessionId,

        @Schema(description = "저장된 단어 또는 표현", example = "significant")
        String word,

        @Schema(description = "단어 뜻 / 설명", example = "중요한, 의미 있는 (= important보다 격식 있는 표현)")
        String definition,

        @Schema(description = "예문", example = "This is a significant improvement in your fluency.")
        String example,

        @Schema(description = "저장 시각")
        LocalDateTime savedAt
) {
    public static WordBookmarkResponse from(WordBookmark bookmark) {
        return new WordBookmarkResponse(
                bookmark.getId(),
                bookmark.getSessionId(),
                bookmark.getWord(),
                bookmark.getDefinition(),
                bookmark.getExample(),
                bookmark.getSavedAt()
        );
    }
}

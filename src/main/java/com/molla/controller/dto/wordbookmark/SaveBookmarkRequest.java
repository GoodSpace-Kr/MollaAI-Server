package com.molla.controller.dto.wordbookmark;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "단어 저장 요청")
public record SaveBookmarkRequest(

        @Schema(description = "연결할 통화 세션 ID (없으면 null)", example = "550e8400-e29b-41d4-a716-446655440000")
        String sessionId,

        @Schema(description = "저장할 단어 또는 표현", example = "significant")
        @NotBlank(message = "단어를 입력해주세요.")
        @Size(max = 100, message = "단어는 100자 이하여야 합니다.")
        String word,

        @Schema(description = "단어 뜻 / 설명", example = "중요한, 의미 있는 (= important보다 격식 있는 표현)")
        String definition,

        @Schema(description = "예문", example = "This is a significant improvement in your fluency.")
        String example
) {}

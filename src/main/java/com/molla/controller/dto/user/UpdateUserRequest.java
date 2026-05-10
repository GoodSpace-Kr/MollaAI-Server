package com.molla.controller.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "내 정보 수정 요청 — null 필드는 수정하지 않음")
public record UpdateUserRequest(

        @Schema(description = "변경할 닉네임", example = "김철수")
        @Size(min = 2, max = 50, message = "닉네임은 2~50자입니다.")
        String username,

        @Schema(description = "영어 레벨 (beginner / intermediate / advanced)", example = "intermediate")
        String englishLevel
) {}

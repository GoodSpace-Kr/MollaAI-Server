package com.molla.controller.dto.inquiry;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "문의하기 요청")
public record CreateInquiryRequest(

        @Schema(description = "이름 (선택)", example = "홍길동")
        @Size(max = 50, message = "이름은 50자 이하여야 합니다.")
        String name,

        @Schema(description = "이메일 (선택)", example = "user@example.com")
        @Size(max = 100, message = "이메일은 100자 이하여야 합니다.")
        String email,

        @Schema(description = "문의 내용 (필수)", example = "서비스 이용 중 불편한 점이 있어요.")
        @NotBlank(message = "문의 내용을 입력해주세요.")
        @Size(max = 2000, message = "문의 내용은 2000자 이하여야 합니다.")
        String content
) {}

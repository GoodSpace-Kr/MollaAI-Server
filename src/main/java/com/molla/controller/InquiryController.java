package com.molla.controller;

import com.molla.common.response.ApiResponse;
import com.molla.controller.dto.inquiry.CreateInquiryRequest;
import com.molla.controller.dto.inquiry.InquiryResponse;
import com.molla.domain.inquiry.Inquiry;
import com.molla.domain.inquiry.InquiryRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Tag(name = "Inquiry", description = "문의하기 API")
@RestController
@RequiredArgsConstructor
public class InquiryController {

    private final InquiryRepository inquiryRepository;

    @Operation(summary = "문의 접수", description = "이름, 이메일은 선택, 문의 내용은 필수입니다.")
    @PostMapping("/api/v1/inquiries")
    public ResponseEntity<ApiResponse<InquiryResponse>> create(
            @RequestBody @Valid CreateInquiryRequest request
    ) {
        Inquiry inquiry = Inquiry.create(request.name(), request.email(), request.content());
        inquiryRepository.save(inquiry);
        log.info("문의 접수 — id: {}, name: {}", inquiry.getId(), inquiry.getName());
        return ResponseEntity.ok(ApiResponse.success("문의가 접수되었습니다.", InquiryResponse.from(inquiry)));
    }

    @Operation(summary = "[관리자] 문의 목록 조회")
    @GetMapping("/api/v1/admin/inquiries")
    public ResponseEntity<ApiResponse<List<InquiryResponse>>> list() {
        List<InquiryResponse> list = inquiryRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(InquiryResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @Operation(summary = "[관리자] 문의 읽음 처리")
    @PatchMapping("/api/v1/admin/inquiries/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markRead(@PathVariable String id) {
        inquiryRepository.findById(id).ifPresent(i -> {
            i.markRead();
            inquiryRepository.save(i);
        });
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}

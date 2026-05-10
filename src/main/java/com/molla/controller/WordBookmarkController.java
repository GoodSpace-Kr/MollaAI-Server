package com.molla.controller;

import com.molla.common.response.ApiResponse;
import com.molla.controller.dto.wordbookmark.SaveBookmarkRequest;
import com.molla.controller.dto.wordbookmark.WordBookmarkResponse;
import com.molla.domain.wordbookmark.WordBookmarkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "WordBookmark", description = "단어장 API — 단어 저장/조회/삭제")
@RestController
@RequiredArgsConstructor
public class WordBookmarkController {

    private final WordBookmarkService wordBookmarkService;

    @Operation(
            summary = "단어장 목록 조회",
            description = "JWT로 인증된 유저의 단어장 전체를 저장 시각 최신순으로 반환합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = WordBookmarkResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "인증 필요",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    @GetMapping("/api/v1/bookmarks")
    public ResponseEntity<ApiResponse<List<WordBookmarkResponse>>> getBookmarks() {
        String userId = getCurrentUserId();
        List<WordBookmarkResponse> response = wordBookmarkService.getBookmarks(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(
            summary = "단어 저장",
            description = """
                    단어 또는 표현을 단어장에 저장합니다.
                    - sessionId를 함께 보내면 어느 통화에서 저장했는지 연결됩니다.
                    - sessionId가 없으면 null로 저장됩니다.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "저장 성공",
                    content = @Content(schema = @Schema(implementation = WordBookmarkResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "요청 데이터 오류",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "인증 필요",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    @PostMapping("/api/v1/bookmarks")
    public ResponseEntity<ApiResponse<WordBookmarkResponse>> saveBookmark(
            @RequestBody @Valid SaveBookmarkRequest request
    ) {
        String userId = getCurrentUserId();
        WordBookmarkResponse response = wordBookmarkService.saveBookmark(userId, request);
        return ResponseEntity.ok(ApiResponse.success("단어가 저장되었습니다.", response));
    }

    @Operation(
            summary = "단어 삭제",
            description = """
                    단어장 항목을 삭제합니다.
                    - 본인이 저장한 단어만 삭제할 수 있습니다.
                    - 다른 유저의 단어를 삭제하려 하면 403 반환합니다.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "단어장 항목 없음",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403", description = "본인 단어장만 삭제 가능",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "인증 필요",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    @DeleteMapping("/api/v1/bookmarks/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteBookmark(
            @PathVariable String id
    ) {
        String userId = getCurrentUserId();
        wordBookmarkService.deleteBookmark(id, userId);
        return ResponseEntity.ok(ApiResponse.success("단어가 삭제되었습니다.", null));
    }

    private String getCurrentUserId() {
        return (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}

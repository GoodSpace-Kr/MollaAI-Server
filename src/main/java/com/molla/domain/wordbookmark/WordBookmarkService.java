package com.molla.domain.wordbookmark;

import com.molla.common.response.ErrorCode;
import com.molla.controller.dto.wordbookmark.SaveBookmarkRequest;
import com.molla.controller.dto.wordbookmark.WordBookmarkResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WordBookmarkService {

    private final WordBookmarkRepository wordBookmarkRepository;

    // ──────────────────────────────────────────────
    // 단어장 목록 조회
    // ──────────────────────────────────────────────

    public List<WordBookmarkResponse> getBookmarks(String userId) {
        return wordBookmarkRepository.findByUserIdOrderBySavedAtDesc(userId)
                .stream()
                .map(WordBookmarkResponse::from)
                .toList();
    }

    // ──────────────────────────────────────────────
    // 단어 저장
    // ──────────────────────────────────────────────

    public WordBookmarkResponse saveBookmark(String userId, SaveBookmarkRequest request) {
        WordBookmark bookmark = WordBookmark.create(
                userId,
                request.sessionId(),
                request.word(),
                request.definition(),
                request.example()
        );

        wordBookmarkRepository.save(bookmark);

        log.info("단어 저장 완료 — userId: {}, word: {}", userId, request.word());
        return WordBookmarkResponse.from(bookmark);
    }

    // ──────────────────────────────────────────────
    // 단어 삭제
    // ──────────────────────────────────────────────

    public void deleteBookmark(String bookmarkId, String userId) {
        WordBookmark bookmark = wordBookmarkRepository.findById(bookmarkId)
                .orElseThrow(() -> new WordBookmarkException(ErrorCode.BOOKMARK_NOT_FOUND));

        if (!bookmark.isOwnedBy(userId)) {
            throw new WordBookmarkException(ErrorCode.UNAUTHORIZED_BOOKMARK_ACCESS);
        }

        wordBookmarkRepository.delete(bookmark);

        log.info("단어 삭제 완료 — bookmarkId: {}, userId: {}", bookmarkId, userId);
    }
}

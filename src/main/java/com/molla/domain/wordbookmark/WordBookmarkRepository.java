package com.molla.domain.wordbookmark;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WordBookmarkRepository extends JpaRepository<WordBookmark, String> {

    /** 유저의 단어장 전체 — 저장 시각 최신순 */
    List<WordBookmark> findByUserIdOrderBySavedAtDesc(String userId);
}

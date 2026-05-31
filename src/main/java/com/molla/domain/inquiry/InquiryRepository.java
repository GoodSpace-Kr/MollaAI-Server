package com.molla.domain.inquiry;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InquiryRepository extends JpaRepository<Inquiry, String> {
    List<Inquiry> findAllByOrderByCreatedAtDesc();

    long countByReadFalse();
}

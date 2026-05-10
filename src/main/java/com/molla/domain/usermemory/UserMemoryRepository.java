package com.molla.domain.usermemory;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserMemoryRepository extends JpaRepository<UserMemory, String> {

    Optional<UserMemory> findByUserId(String userId);
}

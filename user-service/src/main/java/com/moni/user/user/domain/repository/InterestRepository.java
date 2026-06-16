package com.moni.user.user.domain.repository;

import com.moni.user.user.domain.entity.Interest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InterestRepository extends JpaRepository<Interest, UUID> {

    List<Interest> findAllByUserIdAndDeletedAtIsNull(UUID userId);

    void deleteAllByUserId(UUID userId);
}
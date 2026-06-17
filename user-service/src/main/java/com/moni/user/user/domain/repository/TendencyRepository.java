package com.moni.user.user.domain.repository;

import com.moni.user.user.domain.entity.Tendency;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TendencyRepository extends JpaRepository<Tendency, UUID> {

    Optional<Tendency> findByUserIdAndDeletedAtIsNull(UUID userId);

    boolean existsByUserIdAndDeletedAtIsNull(UUID userId);
}
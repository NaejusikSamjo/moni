package com.moni.ai.domain.repository;

import com.moni.ai.domain.entity.AiLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AiLogRepository extends JpaRepository<AiLogEntity, UUID> {
}

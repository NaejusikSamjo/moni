package com.moni.ai.domain.repository;

import com.moni.ai.domain.entity.NewsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface NewsRepository extends JpaRepository<NewsEntity, UUID> {
}

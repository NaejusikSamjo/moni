package com.moni.ai.domain.repository;

import com.moni.ai.domain.entity.NewsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;
import java.util.Optional;

public interface NewsRepository extends JpaRepository<NewsEntity, UUID>, NewsRepositoryCustom {
    boolean existsByUrl(String url);

    List<NewsEntity> findByTicker(String number);

    Optional<NewsEntity> findFirstByTicker(String ticker);
}

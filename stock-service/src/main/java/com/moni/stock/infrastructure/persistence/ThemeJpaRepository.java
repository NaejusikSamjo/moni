package com.moni.stock.infrastructure.persistence;

import com.moni.stock.infrastructure.persistence.entity.ThemeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ThemeJpaRepository extends JpaRepository<ThemeEntity, UUID> {

}
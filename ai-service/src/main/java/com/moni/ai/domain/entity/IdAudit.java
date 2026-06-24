package com.moni.ai.domain.entity;

import com.fasterxml.uuid.Generators;
import com.moni.common.JpaAuditing.baseEntity.BaseEntity;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;

import java.util.UUID;

@MappedSuperclass
public class IdAudit extends BaseEntity {

    @Id
    private UUID id;

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = Generators.timeBasedEpochGenerator().generate();
        }
    }
}

package com.moni.portfolio.domain.support;

import com.fasterxml.uuid.Generators;

import java.util.UUID;

public final class UuidV7 {

    private UuidV7() {
    }

    public static UUID generate() {
        return Generators.timeBasedEpochGenerator().generate();
    }
}

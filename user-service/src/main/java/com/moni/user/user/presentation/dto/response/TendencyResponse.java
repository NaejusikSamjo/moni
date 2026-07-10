package com.moni.user.user.presentation.dto.response;

import com.moni.user.user.domain.entity.Tendency;
import com.moni.user.user.domain.enums.TendencyType;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class TendencyResponse {

    private UUID id;
    private int score;
    private TendencyType type;

    public static TendencyResponse from(Tendency tendency) {
        return TendencyResponse.builder()
                .id(tendency.getId())
                .score(tendency.getScore())
                .type(tendency.getType())
                .build();
    }
}
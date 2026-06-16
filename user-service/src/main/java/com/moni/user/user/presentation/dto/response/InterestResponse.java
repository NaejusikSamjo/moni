package com.moni.user.user.presentation.dto.response;

import com.moni.user.user.domain.entity.Interest;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class InterestResponse {

    private UUID id;
    private String category;

    public static InterestResponse from(Interest interest) {
        return InterestResponse.builder()
                .id(interest.getId())
                .category(interest.getCategory())
                .build();
    }

    public static List<InterestResponse> fromList(List<Interest> interests) {
        return interests.stream()
                .map(InterestResponse::from)
                .toList();
    }
}